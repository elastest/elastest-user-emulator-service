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
import io.elastest.epm.client.service.EpmService;
import io.elastest.eus.EusException;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.ElasTestWebdriverScript;
import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.json.WebDriverError;
import io.elastest.eus.json.WebDriverScriptBody;
import io.elastest.eus.json.WebDriverSessionResponse;
import io.elastest.eus.json.WebDriverSessionValue;
import io.elastest.eus.json.WebDriverStatus;
import io.elastest.eus.platform.service.PlatformService;
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

    // Defined as String instead of integer for testing purposes (inject with
    // @TestPropertySource)
    @Value("${hub.timeout}")
    private String hubTimeout;

    @Value("${browser.screen.resolution}")
    private String browserScreenResolution;

    @Value("${browser.timezone}")
    private String browserTimezone;

    @Value("${webdriver.session.message}")
    private String webdriverSessionMessage;

    @Value("${webdriver.navigation.get.message}")
    private String webdriverNavigationGetMessage;

    @Value("${webdriver.execute.script.message}")
    private String webdriverExecuteScriptMessage;

    @Value("${webdriver.execute.async.script.message}")
    private String webdriverExecuteAsyncScriptMessage;

    @Value("${et.intercept.script.prefix}")
    private String etInterceptScriptPrefix;

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

    @Value("${et.files.path.in.host}")
    private String filesPathInHost;

    @Value("${et.shared.folder}")
    private String eusFilesPath;

    @Value("${container.recording.folder}")
    private String containerRecordingFolder;

    @Value("${et.data.in.host}")
    private String etDataInHost;

    /* *** ET container labels *** */
    @Value("${et.type.label}")
    public String etTypeLabel;

    @Value("${et.tjob.id.label}")
    public String etTJobIdLabel;

    @Value("${et.tjob.exec.id.label}")
    public String etTJobExecIdLabel;

    @Value("${et.tjob.sut.service.name.label}")
    public String etTJobSutServiceNameLabel;

    @Value("${et.tjob.tss.id.label}")
    public String etTJobTSSIdLabel;

    @Value("${et.tjob.tss.type.label}")
    public String etTJobTssTypeLabel;

    @Value("${et.type.test.label.value}")
    public String etTypeTestLabelValue;

    @Value("${et.type.sut.label.value}")
    public String etTypeSutLabelValue;

    @Value("${et.type.tss.label.value}")
    public String etTypeTSSLabelValue;

    @Value("${et.type.core.label.value}")
    public String etTypeCoreLabelValue;

    @Value("${et.type.te.label.value}")
    public String etTypeTELabelValue;

    @Value("${et.type.monitoring.label.value}")
    public String etTypeMonitoringLabelValue;

    @Value("${et.type.tool.label.value}")
    public String etTypeToolLabelValue;
    /* *** END of ET container labels *** */

    String etInstrumentationKey = "elastest-instrumentation";

    private PlatformService platformService;
    private DockerHubService dockerHubService;
    private EusJsonService jsonService;
    private SessionService sessionService;
    private RecordingService recordingService;
    private TimeoutService timeoutService;
    private DynamicDataService dynamicDataService;

    private Map<String, ExecutionData> executionsMap = new HashMap<>();

    @Autowired
    public WebDriverService(PlatformService platformService,
            DockerHubService dockerHubService, EusJsonService jsonService,
            SessionService sessionService, RecordingService recordingService,
            TimeoutService timeoutService,
            DynamicDataService dynamicDataService) {
        this.platformService = platformService;
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
                dockerHubService.getBrowsers(true));
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
                filesPathInHost, dockerNetwork);
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
            logger.error(
                    "Request from Execution received but execution key {} not registered",
                    executionKey);
            return new ResponseEntity<>(executionKey, HttpStatus.BAD_REQUEST);
        }

        ExecutionData data = executionsMap.get(executionKey);
        logger.debug("Request from Execution {} received",
                data.gettJobExecId());

        String requestContext = parseRequestContext(getRequestContext(request));

        List<String> networks = new ArrayList<>();
        String network = dockerNetwork;
        String sutPrefix = data.getSutContainerPrefix();

        if (data.isUseSutNetwork() && sutPrefix != null
                && !"".equals(sutPrefix)) {
            logger.debug("Sut prefix: {}", sutPrefix);

            List<String> sutNetworks = platformService
                    .getContainerNetworksByContainerPrefix(sutPrefix);
            if (sutNetworks.size() > 0) {
                logger.debug("Sut networks: {}", sutNetworks);

                if (sutNetworks != null) {
                    network = sutNetworks.get(0);
                    boolean first = true;
                    for (String currentNetwork : sutNetworks) {
                        if (!first && currentNetwork != null) {
                            networks.add(currentNetwork);
                        }
                        first = false;
                    }
                }
                if (sutNetworks == null || sutNetworks.size() == 0
                        || network == null || "".equals(network)) {
                    network = dockerNetwork;
                    logger.error(
                            "Error on get Sut network to use with External TJob. Using default ElasTest network  {}",
                            dockerNetwork);
                }
                logger.debug("First Sut network: {}", network);
                logger.debug("Sut additional networks: {}", networks);
            }
        }

        return this.session(httpEntity, requestContext, request.getMethod(),
                data.getMonitoringIndex(), data.isWebRtcStatsActivated(),
                etDataInHost + data.getFolderPath(),
                etDataInHost + data.getFolderPath(), network, data, networks);
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
        return this.session(httpEntity, requestContext, requestMethod,
                dynamicDataService.getDefaultEtMonExec(), webRtcActivated,
                filesPathInHost, null, dockerNetwork, null, null);
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            String requestContext, String requestMethod, String monitoringIndex,
            boolean webRtcActivated, String folderPath,
            String sessionFolderPath, String network, ExecutionData execData,
            List<String> additionalNetworks) throws DockerException, Exception {
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
            logger.debug("Is create session" + (execData != null
                    ? " from execution " + execData.gettJobExecId()
                    : ""));

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
            liveSession = sessionService.isLive(requestBody);
            sessionInfo = startBrowser(newRequestBody, requestBody, folderPath,
                    sessionFolderPath, network, execData, additionalNetworks);
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

        sessionInfo.setElastestExecutionData(execData);

        if (isExecuteScript(method, requestContext)) {
            // Execute Script to intercept by EUS and finish
            boolean isIntercepted = interceptScriptIfIsNecessary(requestBody,
                    sessionInfo);
            if (isIntercepted) {
                return new ResponseEntity<>(
                        "ElasTest script intercepted successfully",
                        HttpStatus.OK);
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
                logger.debug("post Url is activated. webRtcActivated={}",
                        webRtcActivated);
                this.manageWebRtcMonitoring(sessionInfo, webRtcActivated,
                        monitoringIndex);
            }
            if (exchangeAgain) {
                if (numRetries < createSessionRetries) {
                    logger.debug("Stopping browser and starting new one {}",
                            sessionInfo);
                    stopBrowser(sessionInfo);
                    sessionInfo = startBrowser(newRequestBody, requestBody,
                            filesPathInHost, sessionFolderPath, network,
                            execData, additionalNetworks);
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
                logger.error("Exception on window maximize with '{}'",
                        maximizeChrome, e);
                logger.debug("Trying with '{}'", maximizeOther);

                try {
                    exchange(httpEntity,
                            requestContext + "/" + sessionInfo.getSessionId()
                                    + maximizeOther,
                            method, sessionInfo, optionalHttpEntity, false);
                } catch (Exception e1) {
                    logger.error("Exception on window maximize with '{}' too",
                            maximizeOther, e1);
                }
            }
            // Start Recording if not is manual recording
            if (!sessionInfo.isManualRecording()) {
                // Start Recording
                logger.debug("Session with automatic recording");
                recordingService.startRecording(sessionInfo);
            }
        }

        // Handle timeout
        handleTimeout(requestContext, method, sessionInfo, isCreateSession,
                monitoringIndex);

        // Send Hub Container name too

        String jqSetHubContainerName = "walk(if type == \"object\" then .hubContainerName += \""
                + sessionInfo.getHubContainerName() + "\"  else . end)";

        responseBody = jsonService.processJsonWithJq(responseBody,
                jqSetHubContainerName);

        return new ResponseEntity<>(responseBody, responseStatus);
    }

    public boolean interceptScriptIfIsNecessary(String requestBody,
            SessionInfo sessionInfo) {
        try {
            WebDriverScriptBody scriptObj = new WebDriverScriptBody(
                    requestBody);

            // Starts with prefix or 'prefix
            String scriptContent = scriptObj.getScript();
            boolean startsWithEtScriptPrefix = scriptContent
                    .startsWith(etInterceptScriptPrefix)
                    || scriptContent.startsWith("'" + etInterceptScriptPrefix);

            if (startsWithEtScriptPrefix) {
                String scriptData = scriptContent;
                // Remove start/end ' char
                if (scriptData.startsWith("'") && scriptData.endsWith("'")) {
                    scriptData = scriptData.substring(1,
                            scriptContent.length() - 1);
                }

                ObjectMapper mapper = new ObjectMapper();
                ElasTestWebdriverScript etScript = mapper.readValue(scriptData,
                        ElasTestWebdriverScript.class);
                if (etScript.isStartTestCommand()) {
                    if (!etScript.getArgs().isEmpty()
                            && etScript.getArgs().containsKey("testName")) {

                        try {
                            logger.debug(
                                    "Intercepted 'Start Test' Script. Restarting recording...");

                            // Stop
                            recordingService.stopRecording(sessionInfo);
                            recordingService.storeMetadata(sessionInfo);

                            // If no testName, delete recording
                            if (sessionInfo.getTestName() == null) {
                                try {
                                    Thread.sleep(1500);
                                } catch (Exception e) {
                                }
                                if (sessionInfo.getFolderPath() == null) {

                                    recordingService.deleteRecording(
                                            sessionInfo.getSessionId());
                                } else {
                                    recordingService.deleteRecording(
                                            sessionInfo.getSessionId(),
                                            sessionInfo.getFolderPath());
                                }
                            }
                            sessionService
                                    .sendRemoveSessionToAllClients(sessionInfo);

                            // Set test name
                            sessionInfo.setTestName((String) etScript.getArgs()
                                    .get("testName"));

                            // Start recording
                            sessionService
                                    .sendNewSessionToAllClients(sessionInfo);
                            recordingService.startRecording(sessionInfo);
                        } catch (Exception e) {
                            throw new Exception("Error on restart recording",
                                    e);
                        }

                    }
                }

                return true;
            }

        } catch (Exception e) {
            logger.error("Error on intercept script", e);
        }
        return false;
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

                logger.debug("WebRtc Monitoring Response: {}", postResponse);

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
                + sessionInfo.getSessionId() + webdriverExecuteScriptMessage;

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
            SessionInfo sessionInfo, boolean isCreateSession,
            String monitoringIndex) {
        // Browser log thread
        if (isCreateSession) {
            timeoutService.launchLogMonitor(sessionInfo, monitoringIndex);
        }

        boolean disableTimeout = false;
        Integer timeout = Integer.parseInt(hubTimeout);
        if (sessionInfo.getCapabilities() != null
                && sessionInfo.getCapabilities().getElastestTimeout() != null) {
            timeout = sessionInfo.getCapabilities().getElastestTimeout();
            if (timeout == 0) {
                disableTimeout = true;
                logger.debug("Timer disabled for session {}",
                        sessionInfo.getSessionId());
            }
        }
        if (!disableTimeout) {
            logger.debug("Timer enabled for session {} with {} seconds",
                    sessionInfo.getSessionId(), timeout);
            timeoutService.shutdownSessionTimer(sessionInfo);
            final SessionInfo finalSessionInfo = sessionInfo;
            Runnable deleteSession = () -> {
                deleteSession(finalSessionInfo, true);
            };

            if (!isDeleteSessionRequest(method, requestContext)) {
                timeoutService.startSessionTimer(sessionInfo, timeout,
                        deleteSession);
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
        sessionService.sendNewSessionToAllClients(sessionInfo);
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
            String originalRequestBody, String folderPath,
            String sessionFolderPath, String network, ExecutionData execData,
            List<String> additionalNetworks) throws Exception {
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
        SessionInfo sessionInfo = new SessionInfo();

        // Envs
        List<String> envs = asList(
                "SCREEN_RESOLUTION=" + browserScreenResolution,
                "TZ=" + browserTimezone);

        Map<String, String> labels = new HashMap<>();
        labels.put(etTypeLabel, etTypeTSSLabelValue);
        labels.put(etTJobTssTypeLabel, "aux");

        if (execData != null) {
            labels.put(etTJobExecIdLabel, execData.gettJobExecId().toString());
            labels.put(etTJobIdLabel, execData.gettJobId().toString());
        }

        boolean liveSession = sessionService.isLive(requestBody);
        sessionInfo.setBrowser(browserName);
        sessionInfo.setLiveSession(liveSession);
        sessionInfo.setVersion(dockerHubService.getVersionFromImage(imageId));

        SimpleDateFormat dateFormat = new SimpleDateFormat(wsDateFormat);
        sessionInfo.setCreationTime(dateFormat.format(new Date()));
        sessionInfo.setHubBindPort(hubPort);
        sessionInfo.setHubVncBindPort(hubPort);

        sessionInfo.setFolderPath(sessionFolderPath);

        platformService.buildAndRunBrowserInContainer(sessionInfo,
                eusContainerPrefix + hubContainerSufix, originalRequestBody,
                folderPath, network, additionalNetworks, envs, labels,
                capabilities, imageId);
        sessionInfo.setStatusMsg("Ready");

        return sessionInfo;
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

            sessionService.sendRemoveSessionToAllClients(sessionInfo);

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

    private boolean isPostSessionRequest(HttpMethod method,
            String requestContext) {
        String context = new String(requestContext);
        if (context.startsWith("//")) {
            context = context.substring(1);
        }
        return method == POST && context.equals(webdriverSessionMessage);
    }

    private boolean isPostUrlRequest(HttpMethod method, String context) {
        // TODO remove
        logger.debug("Checking if request {} is post url request:", context);
        logger.debug("method == POST: {} | context.endsWith({}): {}",
                method == POST, webdriverNavigationGetMessage,
                context.endsWith(webdriverNavigationGetMessage));

        return method == POST
                && context.endsWith(webdriverNavigationGetMessage);
    }

    private boolean isDeleteSessionRequest(HttpMethod method, String context) {
        int chars = countCharsInString(context, '/');
        return method == DELETE && context.startsWith(webdriverSessionMessage)
                && (chars == 2 || (chars == 3 && context.endsWith("window")));
    }

    private boolean isExecuteScript(HttpMethod method, String requestContext) {
        String context = new String(requestContext);
        if (context.startsWith("/" + webdriverSessionMessage)) {
            context = context.substring(1);
        }

        int chars = countCharsInString(context, '/');

        return method == POST && context.startsWith(webdriverSessionMessage)
                && chars == 3
                && (context.endsWith(webdriverExecuteScriptMessage) || context
                        .endsWith(webdriverExecuteAsyncScriptMessage));
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
