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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
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

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

import io.elastest.eus.api.EusException;
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

    @Value("${novnc.port}")
    private int noVncPort;

    @Value("${novnc.container.sufix}")
    private String noVncContainerSufix;

    @Value("${novnc.autofocus.html}")
    private String vncAutoFocusHtml;

    @Value("${hub.port}")
    private int hubPort;

    @Value("${hub.vnc.port}")
    private int hubVncPort;

    @Value("${hub.container.sufix}")
    private String hubContainerSufix;

    @Value("${hub.vnc.password}")
    private String hubVncPassword;

    @Value("${hub.timeout}")
    private String hubTimeout;

    @Value("${ws.dateformat}")
    private String wsDateFormat;

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
            sessionInfo = starBrowser(requestBody, hubTimeout);
            isLive = jsonService.isLive(requestBody);

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
                httpEntity = new HttpEntity<String>("{\n"
                        + "  \"desiredCapabilities\": {\n"
                        + "    \"browserName\": \"firefox\",\n"
                        + "    \"version\": \"\",\n"
                        + "    \"platform\": \"ANY\"\n" + "  }\n" + "}");
            }
            // -------------

        } else {
            Optional<String> sessionIdFromPath = jsonService
                    .getSessionIdFromPath(requestContext);
            if (sessionIdFromPath.isPresent()) {
                String sessionId = sessionIdFromPath.get();
                sessionInfo = sessionService.getSession(sessionId);
                if (sessionInfo == null) {
                    // Occurs if the given session id is not in the list of
                    // active sessions, meaning the session either does not
                    // exist or that it’s not active.

                    HttpStatus responseStatusNotFound = NOT_FOUND;
                    ResponseEntity<String> responseEntity = new ResponseEntity<>(
                            responseStatusNotFound);

                    log.debug("<< Response: {} ", responseStatusNotFound);

                    return responseEntity;
                }
                isLive = sessionInfo.isLiveSession();

            } else {
                // This is a special case that it is very unlikely to happen (in
                // theory, clients should only call a different operation of
                // POST /session after the first time. The only regular case out
                // of this rule is the command GET /status, which is meaningless
                // in EUS
                String errorMessage = "Command " + method + " " + requestContext
                        + " not valid";
                log.error(errorMessage);
                throw new EusException(errorMessage);
            }
        }

        sessionService.shutdownSessionTimer(sessionInfo);
        sessionService.startSessionTimer(sessionInfo);

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

            if (!isLive) {
                if (sessionInfo.getVncUrl() == null) {
                    getVncUrl(sessionId);
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

            sessionService.deleteSession(sessionInfo, false);
        }

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

        dockerService.startAndWaitContainer(imageId, hubContainerName, env);

        String hubUrl = getHubUrl(hubContainerName);
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

        return sessionInfo;
    }

    public String getHubUrl(String containerName) {
        return "http://" + dockerService.getContainerIpAddress(containerName)
                + ":" + hubPort + "/wd/hub";
    }

    public ResponseEntity<String> getVncUrl(String sessionId) {
        String vncContainerName = dockerService.generateContainerName(
                eusContainerPrefix + noVncContainerSufix);

        InputStream inputStream = this.getClass()
                .getResourceAsStream("/" + vncAutoFocusHtml);
        String originPath;
        try {
            Path tempFile = Files.createTempFile("eus", "autofocus");
            FileUtils.copyInputStreamToFile(inputStream, tempFile.toFile());
            originPath = tempFile.toFile().getAbsolutePath();
        } catch (IOException e) {
            String errorMessage = "There was a problem reading "
                    + vncAutoFocusHtml + " due to " + e.getMessage();
            throw new EusException(errorMessage, e);
        }

        String targetPath = "/root/noVNC/" + vncAutoFocusHtml;
        Volume[] volumes = { new Volume(targetPath) };
        Bind[] binds = { new Bind(originPath, volumes[0], AccessMode.rw) };

        log.trace("Mounting volume {} from {}", targetPath, originPath);

        dockerService.startAndWaitContainerWithVolumes(noVncImageId,
                vncContainerName, volumes, binds);
        String vncContainerIp = dockerService
                .getContainerIpAddress(vncContainerName);

        SessionInfo sessionInfo = sessionService.getSession(sessionId);

        String hubContainerIp = dockerService
                .getContainerIpAddress(sessionInfo.getHubContainerName());
        String vncUrl = "http://" + vncContainerIp + ":" + noVncPort + "/"
                + vncAutoFocusHtml + "?host=" + hubContainerIp + "&port="
                + hubVncPort + "&resize=scale&autoconnect=true&password="
                + hubVncPassword;

        dockerService.waitForHostIsReachable(vncUrl);

        sessionInfo.setVncContainerName(vncContainerName);
        sessionInfo.setVncUrl(vncUrl);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(vncUrl,
                HttpStatus.OK);
        return responseEntity;
    }

    private String sanitizeMessage(String message) {
        return message != null
                ? message.trim().replaceAll(" +", " ").replaceAll("\\r", "")
                        .replaceAll("\\n", "").replaceAll("\\t", "")
                : message;
    }

}
