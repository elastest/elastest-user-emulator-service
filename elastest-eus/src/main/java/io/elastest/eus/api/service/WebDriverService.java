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
package io.elastest.eus.api.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

import io.elastest.eus.api.EusException;

/**
 * Service implementation for W3C WebDriver/JSON Wire Protocol.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class WebDriverService {

    private final Logger log = LoggerFactory.getLogger(WebDriverService.class);

    private static final String EUS_CONTAINER_PREFIX = "eus-";

    private static final String NOVNC_CONTAINER_PREFIX = EUS_CONTAINER_PREFIX
            + "novnc-";
    private static final String NOVNC_IMAGE_ID = "psharkey/novnc";
    private static final String NOVNC_PASSWORD = "secret";
    private static final int NOVNC_PORT = 8080;

    private static final String HUB_CONTAINER_PREFIX = EUS_CONTAINER_PREFIX
            + "hub-";
    private static final int HUB_PORT = 4444;

    private DockerService dockerService;
    private PropertiesService propertiesService;
    private JsonService jsonService;

    private Map<String, SessionInfo> sessionRegistry = new ConcurrentHashMap<>();

    @Value("${server.contextPath}")
    private String contextPath;

    @Autowired
    public WebDriverService(DockerService dockerService,
            PropertiesService propertiesService, JsonService jsonService) {
        this.dockerService = dockerService;
        this.propertiesService = propertiesService;
        this.jsonService = jsonService;
    }

    public ResponseEntity<String> process(HttpEntity<String> httpEntity,
            HttpServletRequest request) {

        StringBuffer requestUrl = request.getRequestURL();
        String requestContext = requestUrl.substring(
                requestUrl.lastIndexOf(contextPath) + contextPath.length());
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        log.debug("{} {}", method, requestContext);

        log.trace(">> Request : {}", httpEntity.getBody());

        SessionInfo sessionInfo;

        // Intercept create session
        if (jsonService.isPostSessionRequest(method, requestContext)) {
            sessionInfo = starBrowser(httpEntity.getBody());

        } else {
            Optional<String> sessionIdFromPath = jsonService
                    .getSessionIdFromPath(requestContext);
            if (sessionIdFromPath.isPresent()) {
                String sessionId = sessionIdFromPath.get();
                log.trace("sessionId: {} -- sessionRegistry: {}", sessionId,
                        sessionRegistry);
                sessionInfo = sessionRegistry.get(sessionId);

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

        String hubUrl = sessionInfo.getHubUrl();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange = restTemplate.exchange(
                hubUrl + requestContext, method, httpEntity, String.class);
        String response = exchange.getBody();

        log.trace("<< Response: {}", response);

        // Intercept again create session
        if (jsonService.isPostSessionRequest(method, requestContext)) {
            String sessionId = jsonService.getSessionIdFromResponse(response);
            sessionRegistry.put(sessionId, sessionInfo);
        }

        ResponseEntity<String> responseEntity = new ResponseEntity<>(response,
                HttpStatus.OK);

        log.debug("ResponseEntity {}", responseEntity);

        // Intercept destroy session
        if (jsonService.isDeleteSessionRequest(method, requestContext)) {
            log.trace("Intercepted DELETE session");

            String hubContainerName = sessionInfo.getHubContainerName();
            dockerService.stopAndRemoveContainer(hubContainerName);

            // TODO: Implement a timeout mechanism just in case this command is
            // never invoked
        }

        return responseEntity;
    }

    public SessionInfo starBrowser(String jsonCapabilities) {
        String browserName = jsonService.getBrowser(jsonCapabilities);
        String version = jsonService.getVersion(jsonCapabilities);
        String platform = jsonService.getPlatform(jsonCapabilities);
        String imageId = propertiesService
                .getDockerImageFromCapabilities(browserName, version, platform);

        String hubContainerName = dockerService
                .generateContainerName(HUB_CONTAINER_PREFIX);

        log.debug("Starting browser with container name {}", hubContainerName);

        dockerService.startAndWaitContainer(imageId, hubContainerName);

        String hubUrl = getHubUrl(hubContainerName);
        dockerService.waitForHostIsReachable(hubUrl);

        log.trace("Container: {} -- Hub URL: {}", hubContainerName, hubUrl);

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setHubUrl(hubUrl);
        sessionInfo.setHubContainerName(hubContainerName);

        return sessionInfo;
    }

    public String getHubUrl(String containerName) {
        return "http://" + dockerService.getContainerIpAddress(containerName)
                + ":" + HUB_PORT + "/wd/hub";
    }

    public ResponseEntity<String> getVncUrl(String sessionId) {
        dockerService.startAndWaitContainer(NOVNC_IMAGE_ID,
                NOVNC_CONTAINER_PREFIX);
        String vncContainerIp = dockerService
                .getContainerIpAddress(NOVNC_CONTAINER_PREFIX);

        String hubContainerIp = dockerService.getContainerIpAddress(
                sessionRegistry.get(sessionId).getHubContainerName());
        int hubContainerPort = HUB_PORT;

        String response = "http://" + vncContainerIp + ":" + NOVNC_PORT
                + "/vnc.html?host=" + hubContainerIp + "&port="
                + hubContainerPort + "&resize=scale&autoconnect=true&password="
                + NOVNC_PASSWORD;

        log.trace("VNC URL: {}", response);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(response,
                HttpStatus.OK);
        return responseEntity;
    }

    class SessionInfo {
        private String hubUrl;
        private String hubContainerName;
        private String vncUrl;
        private String vncContainerName;

        public String getHubUrl() {
            return hubUrl;
        }

        public void setHubUrl(String hubUrl) {
            this.hubUrl = hubUrl;
        }

        public String getHubContainerName() {
            return hubContainerName;
        }

        public void setHubContainerName(String hubContainerName) {
            this.hubContainerName = hubContainerName;
        }

        public String getVncUrl() {
            return vncUrl;
        }

        public void setVncUrl(String vncUrl) {
            this.vncUrl = vncUrl;
        }

        public String getVncContainerName() {
            return vncContainerName;
        }

        public void setVncContainerName(String vncContainerName) {
            this.vncContainerName = vncContainerName;
        }

        @Override
        public String toString() {
            return "SessionInfo [hubUrl=" + hubUrl + ", hubContainerName="
                    + hubContainerName + ", vncUrl=" + vncUrl
                    + ", vncContainerName=" + vncContainerName + "]";
        }

    }

}
