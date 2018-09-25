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

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.HostConfig.Bind;
import com.spotify.docker.client.messages.HostConfig.Bind.Builder;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.model.DockerPullImageProgress;
import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.service.DockerService;
import io.elastest.epm.client.service.EpmService;
import io.elastest.eus.EusException;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.json.WebDriverError;
import io.elastest.eus.json.WebDriverSessionResponse;
import io.elastest.eus.json.WebDriverSessionValue;
import io.elastest.eus.json.WebDriverStatus;
import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for W3C WebDriver/JSON Wire Protocol.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class WebDriverService {

    final Logger logger = getLogger(lookup().lookupClass());

    @Value("${et.host.env}")
    private String etHostEnv;

    @Value("${et.host.type.env}")
    private String etHostEnvType;

    @Value("${api.context.path}")
    private String apiContextPath;

    @Value("${eus.container.prefix}")
    private String eusContainerPrefix;

    @Value("${hub.exposedport}")
    private int hubExposedPort;

    @Value("${hub.vnc.exposedport}")
    private int hubVncExposedPort;

    @Value("${hub.novnc.exposedport}")
    private int noVncExposedPort;

    @Value("${hub.container.sufix}")
    private String hubContainerSufix;

    @Value("${novnc.html}")
    private String vncHtml;

    @Value("${hub.vnc.password}")
    private String hubVncPassword;

    // Defined as String instead of integer for testing purposes (inject with
    // @TestPropertySource)
    @Value("${hub.timeout}")
    private String hubTimeout;

    @Value("${browser.shm.size}")
    private long shmSize;

    @Value("${browser.screen.resolution}")
    private String browserScreenResolution;

    @Value("${browser.timezone}")
    private String browserTimezone;

    @Value("${ws.dateformat}")
    private String wsDateFormat;

    @Value("${webdriver.session.message}")
    private String webdriverSessionMessage;

    @Value("${webdriver.navigation.get.message}")
    private String webdriverNavigationGetMessage;

    @Value("${use.torm}")
    private boolean useTorm;

    @Value("${docker.network}")
    private String dockerNetwork;

    @Value("${create.session.timeout.sec}")
    private int createSessionTimeoutSec;

    @Value("${create.session.retries}")
    private int createSessionRetries;

    @Value("${et.config.web.rtc.stats}")
    private String etConfigWebRtcStats;

    @Value("${et.mon.interval}")
    private String etMonInterval;

    @Value("${et.browser.component.prefix}")
    private String etBrowserComponentPrefix;

    @Value("${registry.folder}")
    private String registryFolder;

    @Value("${container.recording.folder}")
    private String containerRecordingFolder;
    
    @Value("${et.data.in.host}")
    private String etDataInHost;

    String etInstrumentationKey = "elastest-instrumentation";

    private DockerService dockerService;
    private DockerHubService dockerHubService;
    private EusJsonService jsonService;
    private SessionService sessionService;
    private RecordingService recordingService;
    private TimeoutService timeoutService;
    private DynamicDataService dynamicDataService;

    private Map<String, ExecutionData> executionsMap = new HashMap<>();

    @Autowired
    public WebDriverService(DockerService dockerService,
            DockerHubService dockerHubService, EusJsonService jsonService,
            SessionService sessionService, RecordingService recordingService,
            TimeoutService timeoutService,
            DynamicDataService dynamicDataService) {
        this.dockerService = dockerService;
        this.dockerHubService = dockerHubService;
        this.jsonService = jsonService;
        this.sessionService = sessionService;
        this.recordingService = recordingService;
        this.timeoutService = timeoutService;
        this.dynamicDataService = dynamicDataService;
    }

    @PreDestroy
    public void cleanUp() {
        // Before shutting down the EUS, all recording files must have been
        // processed
        sessionService.getSessionRegistry()
                .forEach((sessionId, sessionInfo) -> {
                    try {
                        stopBrowser(sessionInfo);
                    } catch (Exception e) {
                        logger.debug("Error on stop browser with session Id {}",
                                sessionId);
                    }
                });
    }

    public Map<String, ExecutionData> getExecutionsMap() {
        return executionsMap;
    }

    public ResponseEntity<String> registerExecution(
            ExecutionData executionData) {
        logger.debug("Registering Execution {}", executionData);
        executionsMap.put(executionData.getKey(), executionData);
        return new ResponseEntity<>(executionData.toString(), OK);
    }

    public ResponseEntity<String> unregisterExecution(String key) {
        try {
            logger.debug("Unregistering Execution with id {}", key);
            executionsMap.remove(key);
            return new ResponseEntity<>(key, OK);
        } catch (Exception e) {
            return new ResponseEntity<>(key,
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> getStatus() throws IOException {
        WebDriverStatus eusStatus = new WebDriverStatus(true, "EUS ready",
                dockerHubService.getBrowsers());
        logger.debug("EUS status {}", eusStatus);
        String statusBody = jsonService.objectToJson(eusStatus);
        return new ResponseEntity<>(statusBody, OK);
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request) throws DockerException, Exception {
        boolean webrtcStatsActivated = etConfigWebRtcStats != null
                && "true".equals(etConfigWebRtcStats);

        String requestContext = getRequestContext(request);

        return this.session(httpEntity, requestContext, request.getMethod(),
                dynamicDataService.getDefaultEtMonExec(), webrtcStatsActivated,
                registryFolder, dockerNetwork);
    }

    public String getRequestContext(HttpServletRequest request) {
        StringBuffer requestUrl = request.getRequestURL();
        logger.debug("{}", requestUrl);
        return requestUrl.substring(requestUrl.lastIndexOf(apiContextPath)
                + apiContextPath.length());
    }

    public ResponseEntity<String> sessionFromExecution(
            HttpEntity<String> httpEntity, HttpServletRequest request,
            String executionKey) throws DockerException, Exception {
        if (!executionsMap.containsKey(executionKey)) {
            return new ResponseEntity<>(executionKey, HttpStatus.BAD_REQUEST);
        }

        ExecutionData data = executionsMap.get(executionKey);
        String requestContext = parseRequestContext(getRequestContext(request));

        String network = dockerNetwork;
        String sutPrefix = data.getSutContainerPrefix();
        if (data.isUseSutNetwork() && sutPrefix != null
                && !"".equals(sutPrefix)) {
            List<Container> containers = dockerService
                    .getContainersByNamePrefix(sutPrefix);
            if (containers != null && containers.size() > 0) {
                network = dockerService.getNetworkName(containers.get(0).id());
                if (network == null || "".equals(network)) {
                    network = dockerNetwork;
                    logger.error(
                            "Error on get Sut network to use with External TJob. Using default ElasTest network  {}",
                            dockerNetwork);
                }
            }
        }

        return this.session(httpEntity, requestContext, request.getMethod(),
                data.getMonitoringIndex(), data.isWebRtcStatsActivated(),
                etDataInHost + data.getFolderPath(), network);
    }

    public String parseRequestContext(String requestContext) {
        requestContext = requestContext.replaceAll("/execution/[^/]*/", "/");
        requestContext = requestContext.replaceAll("//", "/");
        return requestContext;
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            String requestContext, String requestMethod, String monitoringIndex,
            boolean webRtcActivated, String folderPath, String network)
            throws DockerException, Exception {
        HttpMethod method = HttpMethod.resolve(requestMethod);
        String requestBody = jsonService.sanitizeMessage(httpEntity.getBody());

        logger.debug(">> Request: {} {} -- body: {}", method, requestContext,
                requestBody);

        SessionInfo sessionInfo;
        boolean liveSession = false;
        Optional<HttpEntity<String>> optionalHttpEntity = empty();

        // Intercept create session
        boolean isCreateSession = isPostSessionRequest(method, requestContext);
        String newRequestBody = requestBody;
        if (isCreateSession) {

            String browserName = jsonService
                    .jsonToObject(requestBody, WebDriverCapabilities.class)
                    .getDesiredCapabilities().getBrowserName();
            if (browserName == null) {

                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    browserName = objectMapper.readTree(requestBody)
                            .get("capabilities").get("alwaysMatch")
                            .get("browserName").textValue();
                } catch (Exception e) {
                    return new ResponseEntity<String>(
                            "Browser name not recognized in request",
                            HttpStatus.BAD_REQUEST);
                }
            }

            String version = jsonService
                    .jsonToObject(requestBody, WebDriverCapabilities.class)
                    .getDesiredCapabilities().getVersion();

            newRequestBody = processStartSessionRequest(requestBody,
                    browserName);
            httpEntity = new HttpEntity<>(newRequestBody);

            // If live, no timeout
            liveSession = isLive(requestBody);
            sessionInfo = startBrowser(newRequestBody, requestBody, folderPath,
                    network);
            optionalHttpEntity = optionalHttpEntity(newRequestBody, browserName,
                    version);

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

        // Proxy request to browser
        String responseBody = null;
        boolean exchangeAgain = false;
        int numRetries = 0;
        do {
            responseBody = exchange(httpEntity, requestContext, method,
                    sessionInfo, optionalHttpEntity, isCreateSession);
            exchangeAgain = responseBody == null;
            if (this.isPostUrlRequest(method, requestContext)) {
                this.manageWebRtcMonitoring(sessionInfo, webRtcActivated,
                        monitoringIndex);
            }
            if (exchangeAgain) {
                if (numRetries < createSessionRetries) {
                    logger.debug("Stopping browser and starting new one {}",
                            sessionInfo);
                    stopBrowser(sessionInfo);
                    sessionInfo = startBrowser(newRequestBody, requestBody,
                            registryFolder, network);
                    numRetries++;
                    logger.debug(
                            "Problem in POST /session request ... retrying {}/{}",
                            numRetries, createSessionRetries);
                    continue;
                }
                throw new EusException(
                        "Exception creating session in remote browser (num retries "
                                + createSessionRetries + ")");
            }
        } while (exchangeAgain);

        // Handle response
        HttpStatus responseStatus = sessionResponse(requestContext, method,
                sessionInfo, liveSession, responseBody);

        if (isCreateSession) {
            // Maximize Browser Window
            String maximizeChrome = "/window/:windowHandle/maximize";
            String maximizeOther = "/window/maximize";
            try {
                exchange(httpEntity,
                        requestContext + "/" + sessionInfo.getSessionId()
                                + maximizeChrome,
                        method, sessionInfo, optionalHttpEntity, false);
            } catch (Exception e) {
                exchange(httpEntity,
                        requestContext + "/" + sessionInfo.getSessionId()
                                + maximizeOther,
                        method, sessionInfo, optionalHttpEntity, false);
            }
            // Start Recording if not is manual recording
            if (!sessionInfo.isManualRecording()) {
                // Start Recording
                logger.debug("Session with automatic recording");
                recordingService.startRecording(sessionInfo);
            }
        }

        // Handle timeout
        handleTimeout(requestContext, method, sessionInfo, liveSession,
                isCreateSession, monitoringIndex);

        // Send Hub Container name too

        String jqSetHubContainerName = "walk(if type == \"object\" then .hubContainerName += \""
                + sessionInfo.getHubContainerName() + "\"  else . end)";

        responseBody = jsonService.processJsonWithJq(responseBody,
                jqSetHubContainerName);

        return new ResponseEntity<>(responseBody, responseStatus);
    }

    public boolean manageWebRtcMonitoring(SessionInfo sessionInfo,
            boolean activated, String monitoringIndex) {
        boolean manageSuccessful = false;
        if (activated) {
            logger.debug("WebRtc monitoring activated");
            try {
                String configLocalStorageStr = this
                        .getWebRtcMonitoringLocalStorageStr(
                                sessionInfo.getSessionId(), monitoringIndex);
                String postResponse = this.postScript(sessionInfo,
                        configLocalStorageStr, new ArrayList<>());
                manageSuccessful = postResponse != null;

            } catch (JsonProcessingException e) {
                logger.error(
                        "Error on send WebRtc LocalStorage variable for monitoring. {}",
                        e);
            } catch (Exception e) {
                logger.error("Error obtaining monitoring configuration. {}", e);
            }
        }
        return manageSuccessful;
    }

    public JSONObject getWebRtcMonitoringConfig(String sessionId,
            String monitoringIndex) throws Exception {
        String lsSSLHttpApi = dynamicDataService.getLogstashHttpsApi();
        if (lsSSLHttpApi == null || monitoringIndex == null) {
            throw new Exception(
                    "Logstash Http Api Url or Monitoring Execution not found");
        }

        JSONObject configJson = new JSONObject();
        JSONObject elastestInstrumentationJson = new JSONObject();
        JSONObject webRtcJson = new JSONObject();
        String component = etBrowserComponentPrefix + sessionId;

        webRtcJson.put("httpEndpoint", lsSSLHttpApi);
        webRtcJson.put("interval", etMonInterval);

        elastestInstrumentationJson.put("webrtc", webRtcJson);
        elastestInstrumentationJson.put("exec", monitoringIndex);
        elastestInstrumentationJson.put("component", component);

        configJson.put(etInstrumentationKey,
                elastestInstrumentationJson.toString());

        return configJson;
    }

    public String getWebRtcMonitoringLocalStorageStr(String sessionId,
            String monitoringIndex) throws Exception {
        JSONObject config = this.getWebRtcMonitoringConfig(sessionId,
                monitoringIndex);
        String content = config.get(etInstrumentationKey).toString()
                .replace("\"", "\\\"");
        return "localStorage.setItem(\"" + etInstrumentationKey + "\"," + "\""
                + content + "\"" + ");";
    }

    public String postScript(SessionInfo sessionInfo, String script,
            List<Object> args) throws JsonProcessingException, JSONException {
        String requestContext = webdriverSessionMessage + "/"
                + sessionInfo.getSessionId() + "/execute";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject scriptObj = new JSONObject();
        scriptObj.put("script", script);
        scriptObj.put("args", args);
        String body = scriptObj.toString();

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        Optional<HttpEntity<String>> optionalHttpEntity = empty();
        return exchange(httpEntity, requestContext, POST, sessionInfo,
                optionalHttpEntity, false);

    }

    public ResponseEntity<String> getErrorResponse(String message,
            Exception exception) {
        WebDriverError webDriverError = new WebDriverError("EUS internal error",
                message, exception);
        logger.error("{}", webDriverError);
        String errorMessage = message;
        try {
            errorMessage = jsonService.objectToJson(webDriverError);
        } catch (JsonProcessingException e) {
            logger.warn("Exception parsing error message: {} {}", message,
                    exception, e);
        }
        return new ResponseEntity<>(errorMessage, INTERNAL_SERVER_ERROR);
    }

    private void handleTimeout(String requestContext, HttpMethod method,
            SessionInfo sessionInfo, boolean liveSession,
            boolean isCreateSession, String monitoringIndex) {
        // Browser log thread
        if (isCreateSession) {
            String sessionId = sessionInfo.getSessionId();
            String postUrl = sessionInfo.getHubUrl() + "/session/" + sessionId
                    + "/log";
            timeoutService.launchLogMonitor(postUrl, sessionId,
                    monitoringIndex);
        }

        // Only using timer for non-live sessions
        if (!liveSession) {
            timeoutService.shutdownSessionTimer(sessionInfo);
            final SessionInfo finalSessionInfo = sessionInfo;
            Runnable deleteSession = () -> {
                deleteSession(finalSessionInfo, true);
            };

            if (!isDeleteSessionRequest(method, requestContext)) {
                timeoutService.startSessionTimer(sessionInfo,
                        parseInt(hubTimeout), deleteSession);
            }
        }
    }

    private String processStartSessionRequest(String requestBody,
            String browserName) throws IOException {
        String newRequestBody;
        // JSON processing to activate always the browser logging
        String jqActivateBrowserLogging = "walk(if type == \"object\" and .desiredCapabilities then .desiredCapabilities += { \"loggingPrefs\": { \"browser\" : \"ALL\" } }  else . end)";
        newRequestBody = jsonService.processJsonWithJq(requestBody,
                jqActivateBrowserLogging);

        // JSON processing to add binary path if opera
        if (browserName.equalsIgnoreCase("operablink")) {
            String jqOperaBinary = "walk(if type == \"object\" and .desiredCapabilities then .desiredCapabilities += { \"operaOptions\": {\"args\": [], \"binary\": \"/usr/bin/opera\", \"extensions\": [] } }  else . end)";
            newRequestBody = jsonService.processJsonWithJq(newRequestBody,
                    jqOperaBinary);
        }

        // JSON processing to remove banner if chrome
        if (browserName.equalsIgnoreCase("chrome")) {
            // Concat
            String jqChromeBanner = "walk(if type == \"object\" and .desiredCapabilities then .desiredCapabilities.chromeOptions.args += .desiredCapabilities.chromeOptions.args + [\"disable-infobars\"] else . end)";
            newRequestBody = jsonService.processJsonWithJq(newRequestBody,
                    jqChromeBanner);
        }

        // JSON processing to remove browserId
        String jqRemoveBrowserId = "walk(if type == \"object\" then del(.browserId) else . end)";
        newRequestBody = jsonService.processJsonWithJq(newRequestBody,
                jqRemoveBrowserId);

        return newRequestBody;
    }

    private HttpStatus sessionResponse(String requestContext, HttpMethod method,
            SessionInfo sessionInfo, boolean isLive, String responseBody)
            throws Exception {
        HttpStatus responseStatus = OK;

        // Intercept again create session
        if (isPostSessionRequest(method, requestContext)) {
            postSessionRequest(sessionInfo, isLive, responseBody);
        }

        // Intercept destroy session
        if (isDeleteSessionRequest(method, requestContext)) {
            logger.trace("Intercepted DELETE session ({})", method);
            stopBrowser(sessionInfo);
        }

        logger.debug("<< Response: {} -- body: {}", responseStatus,
                responseBody);
        return responseStatus;
    }

    private String exchange(HttpEntity<String> httpEntity,
            String requestContext, HttpMethod method, SessionInfo sessionInfo,
            Optional<HttpEntity<String>> optionalHttpEntity,
            boolean isCreateSession) throws JsonProcessingException {
        String hubUrl = sessionInfo.getHubUrl();

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        if (isCreateSession) {
            int timeoutMillis = (int) SECONDS.toMillis(createSessionTimeoutSec);
            httpRequestFactory.setConnectTimeout(timeoutMillis);
            httpRequestFactory.setConnectionRequestTimeout(timeoutMillis);
            httpRequestFactory.setReadTimeout(timeoutMillis);
        }
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        String finalUrl = hubUrl + requestContext;
        HttpEntity<?> finalHttpEntity = optionalHttpEntity.isPresent()
                ? optionalHttpEntity.get()
                : httpEntity;
        ResponseEntity<String> response = null;
        logger.debug("-> Request to browser: {} {} {}", method, finalUrl,
                finalHttpEntity);
        try {
            response = restTemplate.exchange(finalUrl, method, finalHttpEntity,
                    String.class);
        } catch (Exception e) {
            if (isCreateSession) {
                logger.debug("## Exception exchanging request", e);
                return null;
            } else {
                throw e;
            }
        }
        HttpStatus responseStatusCode = response.getStatusCode();
        String responseBody = response.getBody();
        logger.debug("<- Response from browser: {} {}", responseStatusCode,
                responseBody);

        if (responseStatusCode == FOUND) {
            WebDriverSessionResponse sessionResponse = new WebDriverSessionResponse();
            String path = response.getHeaders().getLocation().getPath();
            sessionResponse
                    .setSessionId(path.substring(path.lastIndexOf('/') + 1));

            responseBody = jsonService.objectToJson(sessionResponse);
        }
        return responseBody;
    }

    private void postSessionRequest(SessionInfo sessionInfo, boolean isLive,
            String responseBody) throws IOException, InterruptedException {
        logger.trace("Session response: JSON: {}", responseBody);
        WebDriverSessionResponse sessionResponse = jsonService
                .jsonToObject(responseBody, WebDriverSessionResponse.class);
        logger.debug("Session response: JSON: {} -- Java: {}", responseBody,
                sessionResponse);

        String sessionId = sessionResponse.getSessionId();
        if (sessionId == null) {
            // Due to changes in JSON response in Selenium 3.5.3
            WebDriverSessionValue responseValue = jsonService
                    .jsonToObject(responseBody, WebDriverSessionValue.class);
            logger.debug("Response value {}", responseValue);
            sessionId = responseValue.getValue().getSessionId();
        }
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setLiveSession(isLive);

        sessionService.putSession(sessionId, sessionInfo);

        if (sessionService.activeWebSocketSessions() && !isLive) {
            sessionService.sendNewSessionToAllClients(sessionInfo);
        }
    }

    private Optional<HttpEntity<String>> optionalHttpEntity(String requestBody,
            String browserName, String version) throws IOException {
        // Workaround due to bug of selenium-server 3.4.0
        // More info on: https://github.com/SeleniumHQ/selenium/issues/3808
        boolean firefoxWithVersion = browserName.equalsIgnoreCase("firefox")
                && (version == null || version.isEmpty());
        boolean betaUnstable = version != null
                && (version.isEmpty() || version.equalsIgnoreCase("latest")
                        || version.equalsIgnoreCase("unstable")
                        || version.equalsIgnoreCase("beta")
                        || version.equalsIgnoreCase("nightly"));
        if (firefoxWithVersion || betaUnstable) {
            String jqRemoveVersionContent = "walk(if type == \"object\" and .version then .version=\"\" else . end)";
            String jsonFirefox = jsonService.processJsonWithJq(requestBody,
                    jqRemoveVersionContent);
            logger.debug("Using capabilities with empty version {}",
                    jsonFirefox);
            return Optional.of(new HttpEntity<String>(jsonFirefox));
        }
        return Optional.empty();
    }

    private ResponseEntity<String> notFound() {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(NOT_FOUND);
        logger.debug("<< Response: {} ({})", responseEntity.getStatusCode(),
                "NOT FOUND");
        return responseEntity;
    }

    public SessionInfo startBrowser(String requestBody,
            String originalRequestBody, String folderPath, String network)
            throws Exception {
        DesiredCapabilities capabilities = jsonService
                .jsonToObject(requestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities();

        String browserName = capabilities.getBrowserName();
        browserName = browserName.equalsIgnoreCase("operablink") ? "opera"
                : browserName;
        String version = capabilities.getVersion();
        String platform = capabilities.getPlatform();
        String imageId = dockerHubService.getBrowserImageFromCapabilities(
                browserName, version, platform);

        logger.info("Using {} as Docker image for {}", imageId, browserName);
        String hubContainerName = dockerService.generateEUSBrowserContainerName(
                eusContainerPrefix + hubContainerSufix);

        // Recording Volume
        List<Bind> volumes = new ArrayList<>();

        Builder dockerSockVolumeBuilder = Bind.builder();
        dockerSockVolumeBuilder.from(folderPath);
        dockerSockVolumeBuilder.to(containerRecordingFolder);

        volumes.add(dockerSockVolumeBuilder.build());

        // Port binding
        Map<String, List<PortBinding>> portBindings = new HashMap<>();

        int hubPort = dockerService.findRandomOpenPort();
        String exposedHubPort = Integer.toString(hubExposedPort);
        portBindings.put(exposedHubPort,
                Arrays.asList(PortBinding.of("0.0.0.0", hubPort)));

        int vncPort = dockerService.findRandomOpenPort();
        String exposedVncPort = Integer.toString(hubVncExposedPort);
        portBindings.put(exposedVncPort,
                Arrays.asList(PortBinding.of("0.0.0.0", vncPort)));

        int noVncBindedPort = dockerService.findRandomOpenPort();
        String exposedNoVncPort = Integer.toString(noVncExposedPort);
        portBindings.put(exposedNoVncPort,
                Arrays.asList(PortBinding.of("0.0.0.0", noVncBindedPort)));

        // Exposed ports
        List<String> exposedPorts = asList(exposedHubPort, exposedVncPort,
                exposedNoVncPort);

        // Envs
        List<String> envs = asList(
                "SCREEN_RESOLUTION=" + browserScreenResolution,
                "TZ=" + browserTimezone);

        DockerBuilder dockerBuilder = new DockerBuilder(imageId);
        dockerBuilder.containerName(hubContainerName);
        dockerBuilder.exposedPorts(exposedPorts);
        dockerBuilder.portBindings(portBindings);
        dockerBuilder.volumeBindList(volumes);
        dockerBuilder.shmSize(shmSize);
        dockerBuilder.envs(envs);
        dockerBuilder.capAdd(asList("SYS_ADMIN"));

        if (useTorm) {
            dockerBuilder.network(network);
        }
        // Save info into SessionInfo
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setHubContainerName(hubContainerName);
        sessionInfo.setBrowser(browserName);
        sessionInfo.setVersion(dockerHubService.getVersionFromImage(imageId));
        SimpleDateFormat dateFormat = new SimpleDateFormat(wsDateFormat);
        sessionInfo.setCreationTime(dateFormat.format(new Date()));
        sessionInfo.setHubBindPort(hubPort);
        sessionInfo.setHubVncBindPort(hubPort);
        sessionInfo.setFolderPath(folderPath);

        String browserId = jsonService
                .jsonToObject(originalRequestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().getBrowserId();
        sessionInfo.setBrowserId(browserId);

        boolean manualRecording = jsonService
                .jsonToObject(originalRequestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().isManualRecording();
        sessionInfo.setManualRecording(manualRecording);

        boolean liveSession = isLive(requestBody);
        sessionInfo.setLiveSession(liveSession);

        sessionInfo.setStatus(DockerServiceStatusEnum.INITIALIZING);
        sessionInfo.setStatusMsg("Initializing...");
        sessionService.sendNewSessionToAllClients(sessionInfo, false);

        // Pull
        dockerService.pullImageWithProgressHandler(imageId,
                getBrowserProgressHandler(imageId, sessionInfo));

        sessionInfo.setStatus(DockerServiceStatusEnum.STARTING);
        sessionInfo.setStatusMsg("Starting...");
        sessionService.sendNewSessionToAllClients(sessionInfo, false);

        // Start
        dockerService.createAndStartContainerWithPull(dockerBuilder.build(),
                EpmService.etMasterSlaveMode, true);

        // Wait Reachable
        String hubPath = "/wd/hub";
        String hubIp = dockerService.getDockerServerIp();
        String hubUrl = "http://" + hubIp + ":" + hubPort + hubPath;

        // Save hub url into Session Info
        sessionInfo.setHubUrl(hubUrl);

        logger.debug("Container: {} -- Hub URL: {}", hubContainerName, hubUrl);

        String vncUrlFormat = "http://%s:%d/" + vncHtml
                + "?resize=scale&autoconnect=true&password=" + hubVncPassword;
        String vncUrl = format(vncUrlFormat, hubIp, noVncBindedPort);
        String internalVncUrl = vncUrl;

        String etHost = getenv(etHostEnv);
        String etHostType = getenv(etHostEnvType);
        if (etHostType != null && etHost != null) {
            // If server-address
            if (!"default".equalsIgnoreCase(etHostType)) {
                hubIp = etHost;
                vncUrl = format(vncUrlFormat, hubIp, noVncBindedPort);
            }
        }

        dockerService.waitForHostIsReachable(internalVncUrl);

        sessionInfo.setVncContainerName(hubContainerName);
        sessionInfo.setVncUrl(vncUrl);
        sessionInfo.setNoVncBindPort(noVncBindedPort);

        sessionInfo.setStatus(DockerServiceStatusEnum.READY);
        sessionInfo.setStatusMsg("Ready");

        return sessionInfo;
    }

    private ProgressHandler getBrowserProgressHandler(String image,
            SessionInfo sessionInfo) {
        DockerPullImageProgress dockerPullImageProgress = new DockerPullImageProgress();
        dockerPullImageProgress.setImage(image);
        dockerPullImageProgress.setCurrentPercentage(0);

        sessionInfo.setStatus(DockerServiceStatusEnum.PULLING);
        sessionInfo.setStatusMsg("Pulling " + image + " image");
        return new ProgressHandler() {
            @Override
            public void progress(ProgressMessage message)
                    throws DockerException {
                dockerPullImageProgress.processNewMessage(message);
                String msg = "Pulling image " + image + ": "
                        + dockerPullImageProgress.getCurrentPercentage() + "%";

                sessionInfo.setStatusMsg(msg);
                if (!sessionInfo.isLiveSession()) {
                    try {
                        sessionService.sendNewSessionToAllClients(sessionInfo,
                                false);
                    } catch (IOException e) {
                        logger.error("Error on send session {} info: ",
                                sessionInfo.getSessionId(), e);
                    }
                }
            }

        };

    }

    public void deleteSession(SessionInfo sessionInfo, boolean timeout) {
        try {
            if (timeout) {
                logger.warn("Deleting session {} due to timeout of {} seconds",
                        sessionInfo.getSessionId(), sessionInfo.getTimeout());
            } else {
                logger.info("Deleting session {}", sessionInfo.getSessionId());
            }

            if (sessionInfo.getVncContainerName() != null) {
                // Stop recording even if manually managed
                recordingService.stopRecording(sessionInfo);
                recordingService.storeMetadata(sessionInfo);
                sessionService.sendRecordingToAllClients(sessionInfo);
            }

            if (!sessionInfo.isLiveSession()) {
                sessionService.sendRemoveSessionToAllClients(sessionInfo);
            }

        } catch (Exception e) {
            logger.error("There was a problem deleting session {}",
                    sessionInfo.getSessionId(), e);
            throw new EusException(e);
        } finally {
            try {
                sessionService.stopAllContainerOfSession(sessionInfo);
            } catch (Exception e) {
                logger.debug("Containers of session {} not removed",
                        sessionInfo.getSessionId());
            }
            sessionService.removeSession(sessionInfo.getSessionId());

            timeoutService.shutdownSessionTimer(sessionInfo);
        }
        if (timeout) {
            throw new EusException("Timeout of " + sessionInfo.getTimeout()
                    + " seconds in session " + sessionInfo.getSessionId());
        }
    }

    private void stopBrowser(SessionInfo sessionInfo) {
        deleteSession(sessionInfo, false);
    }

    private boolean isPostSessionRequest(HttpMethod method, String context) {

        if (context.startsWith("//")) {
            context = context.substring(1);
        }
        return method == POST && context.equals(webdriverSessionMessage);
    }

    private boolean isPostUrlRequest(HttpMethod method, String context) {
        return method == POST
                && context.endsWith(webdriverNavigationGetMessage);
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
        } catch (Exception e) {
            logger.warn(
                    "Exception {} checking if session is live. JSON message: {}",
                    e.getMessage(), jsonMessage);
        }
        logger.trace("Live session = {} -- JSON message: {}", out, jsonMessage);
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

        logger.trace("getSessionIdFromPath -- path: {} sessionId {}", path,
                out);

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
