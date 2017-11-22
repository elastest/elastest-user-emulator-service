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

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static io.elastest.eus.docker.DockerContainer.dockerBuilder;
import static java.lang.Integer.parseInt;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static net.thisptr.jackson.jq.JsonQuery.compile;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;

import io.elastest.eus.docker.DockerContainer.DockerBuilder;
import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.json.WebDriverLog;
import io.elastest.eus.json.WebDriverSessionResponse;
import io.elastest.eus.json.WebDriverSessionValue;
import io.elastest.eus.json.WebDriverStatus;
import io.elastest.eus.session.SessionInfo;
import net.thisptr.jackson.jq.JsonQuery;

/**
 * Service implementation for W3C WebDriver/JSON Wire Protocol.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class WebDriverService {

    final Logger log = getLogger(lookup().lookupClass());

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

    @Value("${webdriver.session.message}")
    private String webdriverSessionMessage;

    @Value("${use.torm}")
    private boolean useTorm;

    @Value("${docker.network}")
    private String dockerNetwork;

    @Value("${log.poll.ms}")
    private int logPollMs;

    @Value("${log.executor.size}")
    private int logExecutorSize;

    private ExecutorService logExecutor;
    private Map<String, Future<?>> logFutureMap;
    private DockerService dockerService;
    private PropertiesService propertiesService;
    private JsonService jsonService;
    private SessionService sessionService;
    private VncService vncService;
    private RecordingService recordingService;
    private LogstashService logstashService;

    @Autowired
    public WebDriverService(DockerService dockerService,
            PropertiesService propertiesService, JsonService jsonService,
            SessionService sessionService, VncService vncService,
            RecordingService recordingService,
            LogstashService logstashService) {
        this.dockerService = dockerService;
        this.propertiesService = propertiesService;
        this.jsonService = jsonService;
        this.sessionService = sessionService;
        this.vncService = vncService;
        this.recordingService = recordingService;
        this.logstashService = logstashService;
    }

    @PostConstruct
    public void init() {
        logExecutor = Executors.newFixedThreadPool(logExecutorSize);
        logFutureMap = new HashMap<>(logExecutorSize);
    }

    @PreDestroy
    public void cleanUp() {
        logExecutor.shutdown();

        // Before shutting down the EUS, all recording files must have been
        // processed
        sessionService.getSessionRegistry()
                .forEach((sessionId, sessionInfo) -> {
                    try {
                        stopBrowser(sessionInfo);
                    } catch (Exception e) {
                        log.error("Exception handling response for session", e);
                    }
                });
    }

    public ResponseEntity<String> getStatus() throws JsonProcessingException {
        WebDriverStatus eusStatus = new WebDriverStatus(true, "EUS ready");
        log.debug("EUS status {}", eusStatus);
        String statusBody = jsonService.objectToJson(eusStatus);
        return new ResponseEntity<>(statusBody, OK);
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request)
            throws IOException, InterruptedException {

        StringBuffer requestUrl = request.getRequestURL();
        String requestContext = requestUrl.substring(
                requestUrl.lastIndexOf(contextPath) + contextPath.length());
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        String requestBody = jsonService.sanitizeMessage(httpEntity.getBody());

        log.debug(">> Request: {} {} -- body: {}", method, requestContext,
                requestBody);

        SessionInfo sessionInfo;
        boolean liveSession = false;
        Optional<HttpEntity<String>> optionalHttpEntity = empty();

        // Intercept create session
        if (isPostSessionRequest(method, requestContext)) {
            // JSON "mangling" to activate always the browser logging
            log.debug("POST requestBody before mangling: {}", requestBody);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode input = objectMapper.readTree(requestBody);
            JsonQuery jsonQuery = compile(
                    "walk(if type == \"object\" and .desiredCapabilities then .desiredCapabilities += { \"loggingPrefs\": { \"browser\" : \"ALL\" } } else . end)");
            requestBody = jsonQuery.apply(input).iterator().next().toString();
            log.debug("POST requestBody after mangling: {}", requestBody);
            httpEntity = new HttpEntity<>(requestBody);

            // If live, no timeout
            liveSession = isLive(requestBody);
            if (liveSession) {
                hubTimeout = "0";
            }
            sessionInfo = startBrowser(requestBody, hubTimeout);
            optionalHttpEntity = optionalHttpEntity(requestBody);

        } else {
            Optional<String> sessionIdFromPath = getSessionIdFromPath(
                    requestContext);
            if (sessionIdFromPath.isPresent()) {
                String sessionId = sessionIdFromPath.get();
                Optional<SessionInfo> optionalSession = sessionService
                        .getSession(sessionId);
                if (optionalSession.isPresent()) {
                    sessionInfo = optionalSession.get();
                } else {
                    return notFound();
                }
                liveSession = sessionInfo.isLiveSession();

            } else {
                return notFound();
            }
        }

        // Only using timer for non-live sessions
        if (!liveSession) {
            sessionService.shutdownSessionTimer(sessionInfo);
            sessionService.startSessionTimer(sessionInfo);
        }

        // Proxy request to Selenium Hub
        String responseBody = exchange(httpEntity, requestContext, method,
                sessionInfo, optionalHttpEntity);

        // Handle response
        HttpStatus responseStatus = sessionResponse(requestContext, method,
                sessionInfo, liveSession, responseBody);

        // Browser log thread
        if (isPostSessionRequest(method, requestContext)) {
            String sessionId = sessionInfo.getSessionId();
            if (!logFutureMap.containsKey(sessionId)) {
                String postUrl = sessionInfo.getHubUrl() + "/session/"
                        + sessionId + "/log";
                logFutureMap.put(sessionId,
                        launchLogMonitor(postUrl, sessionId));
            }
        }

        return new ResponseEntity<>(responseBody, responseStatus);
    }

    private Future<?> launchLogMonitor(String postUrl, String sessionId) {
        log.info("Launching log monitor using URL {}", postUrl);

        return logExecutor.submit(() -> {
            RestTemplate restTemplate = new RestTemplate();
            while (true) {
                try {
                    WebDriverLog response = restTemplate.postForEntity(postUrl,
                            "{\"type\":\"browser\"} ", WebDriverLog.class)
                            .getBody();
                    if (!response.getValue().isEmpty()) {
                        String jsonMessages = logstashService
                                .getJsonMessageFromValueList(
                                        response.getValue());
                        logstashService.sendBrowserConsoleToLogstash(
                                jsonMessages, sessionId);
                    }
                    sleep(logPollMs);

                } catch (Exception e) {
                    log.debug("Termimating log monitor due to {}",
                            e.getMessage());
                    break;
                }
            }
        });
    }

    private HttpStatus sessionResponse(String requestContext, HttpMethod method,
            SessionInfo sessionInfo, boolean isLive, String responseBody) {
        HttpStatus responseStatus = OK;
        try {
            // Intercept again create session
            if (isPostSessionRequest(method, requestContext)) {
                postSessionRequest(sessionInfo, isLive, responseBody);
            }

            // Intercept destroy session
            if (isDeleteSessionRequest(method, requestContext)) {
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
            String responseBody) throws IOException, InterruptedException {
        WebDriverSessionResponse sessionResponse = jsonService
                .jsonToObject(responseBody, WebDriverSessionResponse.class);
        log.debug("Session response: JSON: {} -- Java: {}", responseBody,
                sessionResponse);

        String sessionId = sessionResponse.getSessionId();
        if (sessionId == null) {
            // Due to changes in JSON response in Selenium 3.5.3
            WebDriverSessionValue responseValue = jsonService
                    .jsonToObject(responseBody, WebDriverSessionValue.class);
            log.debug("Response value {}", responseValue);
            sessionId = responseValue.getValue().getSessionId();
        }
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setLiveSession(isLive);

        sessionService.putSession(sessionId, sessionInfo);
        vncService.startVncContainer(sessionInfo);
        recordingService.startRecording(sessionInfo);

        if (sessionService.activeWebSocketSessions() && !isLive) {
            sessionService.sendNewSessionToAllClients(sessionInfo);
        }
    }

    private Optional<HttpEntity<String>> optionalHttpEntity(String requestBody)
            throws IOException {
        // Workaround due to bug of selenium-server 3.4.0
        // More info on: https://github.com/SeleniumHQ/selenium/issues/3808
        String browserName = jsonService
                .jsonToObject(requestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().getBrowserName();
        String version = jsonService
                .jsonToObject(requestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().getVersion();

        if (browserName.equalsIgnoreCase("firefox") && !version.equals("")
                && parseInt(version) < 55) {
            WebDriverCapabilities firefox = new WebDriverCapabilities("firefox",
                    "", "ANY");
            log.debug("Using empty firefox capabilities {}", firefox);
            return Optional.of(
                    new HttpEntity<String>(jsonService.objectToJson(firefox)));
        }
        return Optional.empty();
    }

    private ResponseEntity<String> notFound() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(NOT_FOUND);
        log.debug("<< Response: {} ", responseEntity.getStatusCode());
        return responseEntity;
    }

    private SessionInfo startBrowser(String jsonCapabilities, String timeout)
            throws IOException, InterruptedException {
        String browserName = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getBrowserName();
        String version = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getVersion();
        String platform = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getPlatform();

        String propertiesKey = propertiesService
                .getKeyFromCapabilities(browserName, version, platform);
        String imageId = propertiesService.getDockerImageFromKey(propertiesKey);
        String hubContainerName = dockerService
                .generateContainerName(eusContainerPrefix + hubContainerSufix);
        List<String> env = asList(
                "SE_OPTS=-timeout " + timeout + " -browserTimeout " + timeout);
        log.debug(
                "Starting browser with container name {} and environment variables {}",
                hubContainerName, env);

        // Port binding
        int hubBindPort = dockerService.findRandomOpenPort();
        Binding bindPort = bindPort(hubBindPort);
        ExposedPort exposedPort = tcp(hubExposedPort);

        int hubVncBindPort = dockerService.findRandomOpenPort();
        Binding bindHubVncPort = bindPort(hubVncBindPort);
        ExposedPort exposedHubVncPort = tcp(hubVncExposedPort);

        List<PortBinding> portBindings = asList(
                new PortBinding(bindPort, exposedPort),
                new PortBinding(bindHubVncPort, exposedHubVncPort));

        DockerBuilder dockerBuilder = dockerBuilder(imageId, hubContainerName)
                .portBindings(portBindings).envs(env);
        if (useTorm) {
            dockerBuilder.network(dockerNetwork);
        }
        dockerService.startAndWaitContainer(dockerBuilder.build());

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

    private void stopBrowser(SessionInfo sessionInfo) {
        sessionService.deleteSession(sessionInfo, false);

        String sessionId = sessionInfo.getSessionId();
        if (logFutureMap.containsKey(sessionId)) {
            Future<?> future = logFutureMap.get(sessionId);
            if (!future.isCancelled()) {
                future.cancel(true);
            }
            logFutureMap.remove(sessionId);
        }
    }

    private boolean isPostSessionRequest(HttpMethod method, String context) {
        return method == POST && context.equals(webdriverSessionMessage);
    }

    private boolean isDeleteSessionRequest(HttpMethod method, String context) {
        return method == DELETE && context.startsWith(webdriverSessionMessage)
                && countCharsInString(context, '/') == 2;
    }

    private boolean isLive(String jsonMessage) {
        boolean out = false;
        try {
            out = jsonService
                    .jsonToObject(jsonMessage, WebDriverCapabilities.class)
                    .getDesiredCapabilities().isLive();
            log.trace("Received message from a live session");
        } catch (Exception e) {
            log.trace("Received message from a regular session (non-live)");
        }
        return out;
    }

    public Optional<String> getSessionIdFromPath(String path) {
        Optional<String> out = Optional.empty();
        int i = path.indexOf(webdriverSessionMessage);

        if (i != -1) {
            int j = path.indexOf('/', i + webdriverSessionMessage.length());
            if (j != -1) {
                int k = path.indexOf('/', j + 1);
                int cut = (k == -1) ? path.length() : k;

                String sessionId = path.substring(j + 1, cut);
                out = Optional.of(sessionId);
            }
        }

        log.trace("getSessionIdFromPath -- path: {} sessionId {}", path, out);

        return out;
    }

    public int countCharsInString(String string, char c) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

}
