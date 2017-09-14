/*
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.elastest.eus.service;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for W3C WebDriver/JSON Wire Protocol.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class WebDriverService {

    private final Logger log = LoggerFactory.getLogger(WebDriverService.class);

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${eus.container.prefix}")
    private String eusContainerPrefix;

    @Value("${hub.exposedport}")
    private int hubExposedPort;

    @Value("${hub.vnc.exposedport}")
    private int hubVncExposedPort;

    @Value("${hub.container.sufix}")
    private String hubContainerSufix;

    // Defined as String instead of integer for testing purposes (inject with
    // @TestPropertySource)
    @Value("${hub.timeout}")
    private String hubTimeout;

    @Value("${ws.dateformat}")
    private String wsDateFormat;

    private DockerService dockerService;
    private PropertiesService propertiesService;
    private JsonService jsonService;
    private SessionService sessionService;
    private VncService vncService;
    private RecordingService recordingService;

    @Autowired
    public WebDriverService(DockerService dockerService,
            PropertiesService propertiesService, JsonService jsonService,
            SessionService sessionService, VncService vncService,
            RecordingService recordingService) {
        this.dockerService = dockerService;
        this.propertiesService = propertiesService;
        this.jsonService = jsonService;
        this.sessionService = sessionService;
        this.vncService = vncService;
        this.recordingService = recordingService;
    }

    public ResponseEntity<String> getStatus() {
        String statusBody = jsonService.getStatus().toString();
        return new ResponseEntity<>(statusBody, OK);
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request) throws Exception {

        StringBuffer requestUrl = request.getRequestURL();
        String requestContext = requestUrl.substring(
                requestUrl.lastIndexOf(contextPath) + contextPath.length());
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        String requestBody = jsonService.sanitizeMessage(httpEntity.getBody());

        log.debug(">> Request: {} {} -- body: {}", method, requestContext,
                requestBody);

        SessionInfo sessionInfo;
        boolean isLive = false;
        Optional<HttpEntity<String>> optionalHttpEntity = Optional.empty();

        // Intercept create session
        if (jsonService.isPostSessionRequest(method, requestContext)) {
            isLive = jsonService.isLive(requestBody);

            // If live, no timeout
            if (isLive) {
                hubTimeout = "0";
            }
            sessionInfo = starBrowser(requestBody, hubTimeout);
            optionalHttpEntity = optionalHttpEntity(requestBody);

        } else {
            Optional<String> sessionIdFromPath = jsonService
                    .getSessionIdFromPath(requestContext);
            if (sessionIdFromPath.isPresent()) {
                String sessionId = sessionIdFromPath.get();
                Optional<SessionInfo> optionalSession = sessionService
                        .getSession(sessionId);
                if (optionalSession.isPresent()) {
                    sessionInfo = optionalSession.get();
                } else {
                    return notFound();
                }
                isLive = sessionInfo.isLiveSession();

            } else {
                return notFound();
            }
        }

        // Only using timer for non-live sessions
        if (!isLive) {
            sessionService.shutdownSessionTimer(sessionInfo);
            sessionService.startSessionTimer(sessionInfo);
        }

        // Proxy request to Selenium Hub
        String responseBody = exchange(httpEntity, requestContext, method,
                sessionInfo, optionalHttpEntity);

        // Handle response
        HttpStatus responseStatus = sessionResponse(requestContext, method,
                sessionInfo, isLive, responseBody);

        return new ResponseEntity<>(responseBody, responseStatus);
    }

    private HttpStatus sessionResponse(String requestContext, HttpMethod method,
            SessionInfo sessionInfo, boolean isLive, String responseBody) {
        HttpStatus responseStatus = OK;
        try {
            // Intercept again create session
            if (jsonService.isPostSessionRequest(method, requestContext)) {
                postSessionRequest(sessionInfo, isLive, responseBody);
            }

            // Intercept destroy session
            if (jsonService.isDeleteSessionRequest(method, requestContext)) {
                log.trace("Intercepted DELETE session");
                stopBrowser(sessionInfo);
            }

        } catch (Exception e) {
            log.error("Exception handling response for session", e);
            responseStatus = INTERNAL_SERVER_ERROR;

        } finally {
            log.debug("<< Response: {} -- body: {}", responseStatus,
                    responseBody);
        }
        return responseStatus;
    }

    private String exchange(HttpEntity<String> httpEntity,
            String requestContext, HttpMethod method, SessionInfo sessionInfo,
            Optional<HttpEntity<String>> optionalHttpEntity) {
        String hubUrl = sessionInfo.getHubUrl();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange = restTemplate
                .exchange(hubUrl + requestContext, method,
                        optionalHttpEntity.isPresent()
                                ? optionalHttpEntity.get()
                                : httpEntity,
                        String.class);
        return exchange.getBody();
    }

    private void postSessionRequest(SessionInfo sessionInfo, boolean isLive,
            String responseBody) throws Exception {
        String sessionId = jsonService.getSessionIdFromResponse(responseBody);
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setLiveSession(isLive);

        sessionService.putSession(sessionId, sessionInfo);
        vncService.startVncContainer(sessionInfo);
        recordingService.startRecording(sessionInfo);

        if (sessionService.activeWebSocketSessions() && !isLive) {
            sessionService.sendNewSessionToAllClients(sessionInfo);
        }
    }

    private Optional<HttpEntity<String>> optionalHttpEntity(
            String requestBody) {
        // Workaround due to bug of selenium-server 3.4.0
        // More info on: https://github.com/SeleniumHQ/selenium/issues/3808
        String browserName = jsonService.getBrowser(requestBody);
        String version = jsonService.getVersion(requestBody);

        if (browserName.equalsIgnoreCase("firefox") && !version.equals("")) {
            log.warn(
                    "Due to a bug in selenium-server 3.4.0 the W3C capabilities are not handled correctly");

            return Optional
                    .of(new HttpEntity<String>("{ \"desiredCapabilities\": {"
                            + "\"browserName\": \"firefox\","
                            + "\"version\": \"\","
                            + "\"platform\": \"ANY\" } }"));
        }
        return Optional.empty();
    }

    private ResponseEntity<String> notFound() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(NOT_FOUND);
        log.debug("<< Response: {} ", responseEntity.getStatusCode());
        return responseEntity;
    }

    private SessionInfo starBrowser(String jsonCapabilities, String timeout)
            throws Exception {
        String browserName = jsonService.getBrowser(jsonCapabilities);
        String version = jsonService.getVersion(jsonCapabilities);
        String platform = jsonService.getPlatform(jsonCapabilities);

        String propertiesKey = propertiesService
                .getKeyFromCapabilities(browserName, version, platform);
        String imageId = propertiesService.getDockerImageFromKey(propertiesKey);
        String hubContainerName = dockerService
                .generateContainerName(eusContainerPrefix + hubContainerSufix);
        String[] env = {
                "SE_OPTS=-timeout " + timeout + " -browserTimeout " + timeout };
        log.debug(
                "Starting browser with container name {} and environment variables {}",
                hubContainerName, env);

        // Port binding
        int hubBindPort = dockerService.findRandomOpenPort();
        Binding bindPort = Ports.Binding.bindPort(hubBindPort);
        ExposedPort exposedPort = ExposedPort.tcp(hubExposedPort);

        int hubVncBindPort = dockerService.findRandomOpenPort();
        Binding bindHubVncPort = Ports.Binding.bindPort(hubVncBindPort);
        ExposedPort exposedHubVncPort = ExposedPort.tcp(hubVncExposedPort);

        PortBinding[] portBindings = { new PortBinding(bindPort, exposedPort),
                new PortBinding(bindHubVncPort, exposedHubVncPort) };

        dockerService.startAndWaitContainer(imageId, hubContainerName,
                portBindings, env);

        String hubUrl = "http://" + dockerService.getDockerServerIp() + ":"
                + hubBindPort + "/wd/hub";
        dockerService.waitForHostIsReachable(hubUrl);

        log.trace("Container: {} -- Hub URL: {}", hubContainerName, hubUrl);

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setHubUrl(hubUrl);
        sessionInfo.setHubContainerName(hubContainerName);
        sessionInfo.setBrowser(browserName);
        sessionInfo
                .setVersion(propertiesService.getVersionFromKey(propertiesKey));
        SimpleDateFormat dateFormat = new SimpleDateFormat(wsDateFormat);
        sessionInfo.setCreationTime(dateFormat.format(new Date()));
        sessionInfo.setHubBindPort(hubBindPort);
        sessionInfo.setHubVncBindPort(hubVncBindPort);

        return sessionInfo;
    }

    private void stopBrowser(SessionInfo sessionInfo)
            throws IOException, InterruptedException {
        if (sessionInfo.getVncContainerName() != null) {
            recordingService.stopRecording(sessionInfo);
            recordingService.storeRecording(sessionInfo);
            recordingService.storeMetadata(sessionInfo);

            sessionService.sendRecordingToAllClients(sessionInfo);
        }
        sessionService.deleteSession(sessionInfo, false);
    }

}
