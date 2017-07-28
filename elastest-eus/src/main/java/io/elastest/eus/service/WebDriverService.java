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

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
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

    @Value("${server.contextPath}")
    private String contextPath;

    @Value("${eus.container.prefix}")
    private String eusContainerPrefix;

    @Value("${novnc.image.id}")
    private String noVncImageId;

    @Value("${novnc.exposedport}")
    private int noVncExposedPort;

    @Value("${novnc.container.sufix}")
    private String noVncContainerSufix;

    @Value("${novnc.autofocus.html}")
    private String vncAutoFocusHtml;

    @Value("${hub.exposedport}")
    private int hubExposedPort;

    @Value("${hub.vnc.exposedport}")
    private int hubVncExposedPort;

    @Value("${hub.container.sufix}")
    private String hubContainerSufix;

    @Value("${hub.vnc.password}")
    private String hubVncPassword;

    // Defined as String instead of integer for testing purposes (inject with
    // @TestPropertySource)
    @Value("${hub.timeout}")
    private String hubTimeout;

    @Value("${ws.dateformat}")
    private String wsDateFormat;

    @Value("${registry.folder}")
    private String registryFolder;

    private DockerService dockerService;
    private PropertiesService propertiesService;
    private JsonService jsonService;
    private SessionService sessionService;

    @Autowired
    public WebDriverService(DockerService dockerService,
            PropertiesService propertiesService, JsonService jsonService,
            SessionService sessionService) {
        this.dockerService = dockerService;
        this.propertiesService = propertiesService;
        this.jsonService = jsonService;
        this.sessionService = sessionService;
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request) {

        StringBuffer requestUrl = request.getRequestURL();
        String requestContext = requestUrl.substring(
                requestUrl.lastIndexOf(contextPath) + contextPath.length());
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        String requestBody = sanitizeMessage(httpEntity.getBody());

        log.debug(">> Request: {} {} -- body: {}", method, requestContext,
                requestBody);

        SessionInfo sessionInfo;

        boolean isLive = false;

        // Intercept create session
        if (jsonService.isPostSessionRequest(method, requestContext)) {
            isLive = jsonService.isLive(requestBody);

            // If live, no timeout
            if (isLive) {
                hubTimeout = "0";
            }
            sessionInfo = starBrowser(requestBody, hubTimeout);

            // -------------
            // FIXME: Workaround due to bug of selenium-server 3.4.0
            // More info on: https://github.com/SeleniumHQ/selenium/issues/3808
            String browserName = jsonService.getBrowser(requestBody);
            String version = jsonService.getVersion(requestBody);
            if (browserName.equalsIgnoreCase("firefox")
                    && !version.equals("")) {
                version = "";
                log.warn(
                        "Due to a bug in selenium-server 3.4.0 the W3C capabilities are not handled correctly");
                httpEntity = new HttpEntity<String>(
                        "{ \"desiredCapabilities\": {"
                                + "\"browserName\": \"firefox\","
                                + "\"version\": \"\","
                                + "\"platform\": \"ANY\" } }");
            }
            // -------------

        } else {
            Optional<String> sessionIdFromPath = jsonService
                    .getSessionIdFromPath(requestContext);
            String sessionId = sessionIdFromPath.get();
            sessionInfo = sessionService.getSession(sessionId);
            if (sessionInfo == null) {
                // Occurs if the given session id is not in the list of
                // active sessions, meaning the session either does not
                // exist or that it’s not active.

                return sessionNotFound();
            }
            isLive = sessionInfo.isLiveSession();
        }

        // Only using timer for non-live sessions
        if (!isLive) {
            sessionService.shutdownSessionTimer(sessionInfo);
            sessionService.startSessionTimer(sessionInfo);
        }

        String hubUrl = sessionInfo.getHubUrl();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange = restTemplate.exchange(
                hubUrl + requestContext, method, httpEntity, String.class);
        String responseBody = exchange.getBody();

        // Intercept again create session
        if (jsonService.isPostSessionRequest(method, requestContext)) {
            String sessionId = jsonService
                    .getSessionIdFromResponse(responseBody);
            sessionInfo.setSessionId(sessionId);
            sessionInfo.setLiveSession(isLive);

            sessionService.putSession(sessionId, sessionInfo);

            if (sessionService.activeWebSocketSessions() && !isLive) {
                if (sessionInfo.getVncUrl() == null) {
                    startVncContainer(sessionInfo);
                }
                sessionService.sendNewSessionToAllClients(sessionInfo);
            }
        }

        HttpStatus responseStatusOk = OK;
        ResponseEntity<String> responseEntity = new ResponseEntity<>(
                responseBody, responseStatusOk);

        log.debug("<< Response: {} -- body: {}", responseStatusOk,
                responseBody);

        // Intercept destroy session
        if (jsonService.isDeleteSessionRequest(method, requestContext)) {
            log.trace("Intercepted DELETE session");

            stopBrowser(sessionInfo);
        }

        return responseEntity;
    }

    private void stopBrowser(SessionInfo sessionInfo) {
        String vncContainerName = sessionInfo.getVncContainerName();
        if (vncContainerName != null) {
            stopRecording(vncContainerName);
            getRecordingInMp4Format(sessionInfo);
        }
        sessionService.deleteSession(sessionInfo, false);
    }

    private ResponseEntity<String> sessionNotFound() {
        HttpStatus responseStatusNotFound = NOT_FOUND;
        ResponseEntity<String> responseEntity = new ResponseEntity<>(
                responseStatusNotFound);

        log.debug("<< Response: {} ", responseStatusNotFound);

        return responseEntity;
    }

    public SessionInfo starBrowser(String jsonCapabilities, String timeout) {
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
        int hubBindPort = sessionService.findRandomOpenPort();
        Binding bindPort = Ports.Binding.bindPort(hubBindPort);
        ExposedPort exposedPort = ExposedPort.tcp(hubExposedPort);

        int hubVncBindPort = sessionService.findRandomOpenPort();
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

    private void startRecording(String noNvcContainerName,
            String hubContainerName, String sessionId) {
        String hubContainerIp = dockerService
                .getContainerIpAddress(hubContainerName);
        log.debug("Recording session {} in {}:{}", sessionId, hubContainerIp,
                hubVncExposedPort);
        dockerService.execCommand(noNvcContainerName, false, "flvrec.py", "-P",
                "passwd_file", "-o", sessionId + ".flv", hubContainerIp,
                String.valueOf(hubVncExposedPort));
    }

    private void stopRecording(String noNvcContainerName) {
        log.trace("Stopping recording of container {}", noNvcContainerName);
        dockerService.execCommand(noNvcContainerName, true, "killall",
                "flvrec.py");
    }

    private void getRecordingInMp4Format(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String noNvcContainerName = sessionInfo.getVncContainerName();
        String recordingFileName = sessionId + ".mp4";
        String jsonFileName = sessionId + ".json";

        dockerService.execCommand(noNvcContainerName, true, "ffmpeg", "-i",
                sessionId + ".flv", "-c:v", "libx264", "-crf", "19", "-strict",
                "experimental", recordingFileName);

        try {
            String target = registryFolder + recordingFileName;

            InputStream inputStream = dockerService.getFileFromContainer(
                    noNvcContainerName, recordingFileName);

            // -------------
            // FIXME: Workaround due to strange behavior of docker-java
            // it seems that copyArchiveFromContainerCmd not works correctly

            byte[] bytes = IOUtils.toByteArray(inputStream);

            int i = 0;
            for (; i < bytes.length; i++) {
                char c1 = (char) bytes[i];
                if (c1 == 'f') {
                    char c2 = (char) bytes[i + 1];
                    char c3 = (char) bytes[i + 2];
                    if (c2 == 't' && c3 == 'y') {
                        break;
                    }
                }
            }

            FileUtils.writeByteArrayToFile(new File(target),
                    Arrays.copyOfRange(bytes, i - 4, bytes.length));

            // -------------

            JSONObject sessionInfoToJson = jsonService
                    .sessionInfoToJson(sessionInfo);
            FileUtils.writeStringToFile(new File(registryFolder + jsonFileName),
                    sessionInfoToJson.toString(), Charset.defaultCharset());

        } catch (IOException e) {
            log.error("Exception getting recording (sessiodId {})",
                    sessionInfo.getSessionId(), e);
        }
    }

    public ResponseEntity<String> getVncUrl(String sessionId) {
        SessionInfo sessionInfo = sessionService.getSession(sessionId);
        if (sessionInfo == null) {
            return sessionNotFound();
        }
        startVncContainer(sessionInfo);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(
                sessionInfo.getVncUrl(), HttpStatus.OK);
        return responseEntity;
    }

    private void startVncContainer(SessionInfo sessionInfo) {
        String vncContainerName = dockerService.generateContainerName(
                eusContainerPrefix + noVncContainerSufix);

        // Port binding
        int noVncBindPort = sessionService.findRandomOpenPort();
        Binding bindNoVncPort = Ports.Binding.bindPort(noVncBindPort);
        ExposedPort exposedNoVncPort = ExposedPort.tcp(noVncExposedPort);

        PortBinding[] portBindings = {
                new PortBinding(bindNoVncPort, exposedNoVncPort) };

        dockerService.startAndWaitContainer(noVncImageId, vncContainerName,
                portBindings);

        String vncContainerIp = dockerService.getDockerServerIp();
        String hubContainerIp = dockerService.getDockerServerIp();

        String vncUrl = "http://" + vncContainerIp + ":" + noVncBindPort + "/"
                + vncAutoFocusHtml + "?host=" + hubContainerIp + "&port="
                + sessionInfo.getHubVncBindPort()
                + "&resize=scale&autoconnect=true&password=" + hubVncPassword;

        dockerService.waitForHostIsReachable(vncUrl);

        sessionInfo.setVncContainerName(vncContainerName);
        sessionInfo.setVncUrl(vncUrl);
        sessionInfo.setNoVncBindPort(noVncBindPort);

        startRecording(vncContainerName, sessionInfo.getHubContainerName(),
                sessionInfo.getSessionId());
    }

    public String getStatus() {
        return jsonService.getStatus().toString();
    }

    private String sanitizeMessage(String message) {
        return message != null
                ? message.trim().replaceAll(" +", " ").replaceAll("\\r", "")
                        .replaceAll("\\n", "").replaceAll("\\t", "")
                : message;
    }

}
