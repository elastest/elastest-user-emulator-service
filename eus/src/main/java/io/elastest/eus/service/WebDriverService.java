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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.docker.client.exceptions.DockerException;

import io.elastest.eus.EusException;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.ElasTestWebdriverScript;
import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.json.WebDriverError;
import io.elastest.eus.json.WebDriverScriptBody;
import io.elastest.eus.json.WebDriverSessionResponse;
import io.elastest.eus.json.WebDriverSessionValue;
import io.elastest.eus.json.WebDriverStatus;
import io.elastest.eus.platform.service.PlatformService;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.session.SessionManager;

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
    @Value("${ws.dateformat}")
    private String wsDateFormat;
    @Value("${novnc.html}")
    private String vncHtml;
    @Value("${hub.vnc.password}")
    private String hubVncPassword;
    @Value("${et.host.env}")
    private String etHostEnv;
    @Value("${et.host.type.env}")
    private String etHostEnvType;

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
    @Value("${webdriver.crossbrowser.session.message}")
    private String webdriverCrossbrowserSessionMessage;
    @Value("${webdriver.navigation.get.message}")
    private String webdriverNavigationGetMessage;
    @Value("${webdriver.execute.script.message}")
    private String webdriverExecuteScriptMessage;
    @Value("${webdriver.execute.async.script.message}")
    private String webdriverExecuteAsyncScriptMessage;
    @Value("${et.intercept.script.prefix}")
    private String etInterceptScriptPrefix;
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
    public EusFilesService eusFilesService;

    private Map<String, ExecutionData> executionsMap = new HashMap<>();
    private Map<String, BrowserSync> crossBrowserRegistry = new ConcurrentHashMap<>();

    @Autowired
    public WebDriverService(PlatformService platformService,
            DockerHubService dockerHubService, EusJsonService jsonService,
            SessionService sessionService, RecordingService recordingService,
            TimeoutService timeoutService,
            DynamicDataService dynamicDataService,
            EusFilesService eusFilesService) {
        this.platformService = platformService;
        this.dockerHubService = dockerHubService;
        this.jsonService = jsonService;
        this.sessionService = sessionService;
        this.recordingService = recordingService;
        this.timeoutService = timeoutService;
        this.dynamicDataService = dynamicDataService;
        this.eusFilesService = eusFilesService;
    }

    @PreDestroy
    public void cleanUp() {
        // Before shutting down the EUS, all recording files must have been
        // processed
        sessionService.getSessionRegistry()
                .forEach((sessionId, sessionManager) -> {
                    try {
                        stopBrowser(sessionManager);
                    } catch (Exception e) {
                        logger.debug("Error on stop browser with session Id {}",
                                sessionId);
                    }
                });
        stopCrossBrowserSessions();
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

    public String getRequestContext(HttpServletRequest request) {
        StringBuffer requestUrl = request.getRequestURL();
        logger.debug("Request Url {}", requestUrl);
        return requestUrl.substring(requestUrl.lastIndexOf(apiContextPath)
                + apiContextPath.length());
    }

    public String parseRequestContext(String requestContext) {
        requestContext = requestContext.replaceAll("/execution/[^/]*/", "/");
        requestContext = requestContext.replaceAll("//", "/");
        return requestContext;
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request) throws DockerException, Exception {
        String requestBody = jsonService.sanitizeMessage(httpEntity.getBody());
        String requestContext = getRequestContext(request);
        return session(httpEntity, requestContext, requestBody,
                request.getMethod());
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            String requestContext, String requestBody, String method)
            throws DockerException, Exception {
        boolean webrtcStatsActivated = etConfigWebRtcStats != null
                && "true".equals(etConfigWebRtcStats);

        return this.session(httpEntity, requestContext, method, requestBody,
                dynamicDataService.getDefaultEtMonExec(), webrtcStatsActivated,
                eusFilesService.getFilesPathInHostPath());
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
        String requestBody = jsonService.sanitizeMessage(httpEntity.getBody());
        String requestContext = parseRequestContext(getRequestContext(request));

        return sessionFromExecution(httpEntity, requestContext, requestBody,
                request.getMethod(), data);
    }

    public ResponseEntity<String> sessionFromExecution(
            HttpEntity<String> httpEntity, String requestContext,
            String requestBody, String method, ExecutionData data)
            throws DockerException, Exception {
        logger.debug("Request from Execution {} received",
                data.gettJobExecId());

        return this.session(httpEntity, requestContext, method, requestBody,
                data.getMonitoringIndex(), data.isWebRtcStatsActivated(),
                eusFilesService.getHostSessionFolderFromExecution(data),
                eusFilesService.getHostSessionFolderFromExecution(data), data);
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            String requestContext, String requestMethod, String requestBody,
            String monitoringIndex, boolean webRtcActivated, String folderPath)
            throws DockerException, Exception {
        return this.session(httpEntity, requestContext, requestMethod,
                requestBody, dynamicDataService.getDefaultEtMonExec(),
                webRtcActivated, eusFilesService.getFilesPathInHostPath(), null,
                null);
    }

    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            String requestContext, String requestMethod, String requestBody,
            String monitoringIndex, boolean webRtcActivated, String folderPath,
            String sessionFolderPath, ExecutionData execData)
            throws DockerException, Exception {
        HttpMethod method = HttpMethod.resolve(requestMethod);

        logger.debug(">> Request: {} {} -- body: {}", method, requestContext,
                requestBody);

        SessionManager sessionManager;
        boolean liveSession = false;
        Optional<HttpEntity<String>> optionalHttpEntity = empty();

        // Intercept create session
        boolean isCreateSession = isPostSessionRequest(method, requestContext);
        String newRequestBody = requestBody;
        if (isCreateSession) {
            logger.debug("Is create session" + (execData != null
                    ? " from execution " + execData.gettJobExecId()
                    : ""));

            WebDriverCapabilities webDriverCapabilities = jsonService
                    .jsonToObject(requestBody, WebDriverCapabilities.class);

            String browserName = webDriverCapabilities.getDesiredCapabilities()
                    .getBrowserName();
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

            String version = webDriverCapabilities.getDesiredCapabilities()
                    .getVersion();

            newRequestBody = processStartSessionRequest(requestBody,
                    browserName);
            httpEntity = new HttpEntity<>(newRequestBody);

            // If live, no timeout
            liveSession = sessionService.isLive(requestBody);
            sessionManager = startBrowser(newRequestBody, requestBody,
                    folderPath, sessionFolderPath, execData);
            optionalHttpEntity = optionalHttpEntity(newRequestBody, browserName,
                    version);

        } else {
            Optional<String> sessionIdFromPath = getSessionIdFromPath(
                    requestContext);
            if (sessionIdFromPath.isPresent()) {
                String sessionId = sessionIdFromPath.get();
                Optional<SessionManager> optionalSession = sessionService
                        .getSession(sessionId);
                if (optionalSession.isPresent()) {
                    sessionManager = optionalSession.get();
                } else {
                    return notFound();
                }
                liveSession = sessionManager.isLiveSession();

            } else {
                return notFound();
            }
        }

        sessionManager.setElastestExecutionData(execData);

        if (isExecuteScript(method, requestContext)) {
            // Execute Script to intercept by EUS and finish
            boolean isIntercepted = interceptScriptIfIsNecessary(requestBody,
                    sessionManager);
            if (isIntercepted) {
                String interceptedMsg = "{\"msg\": \"ElasTest script intercepted successfully\"}";
                logger.debug(interceptedMsg);
                return new ResponseEntity<String>(interceptedMsg,
                        HttpStatus.OK);
            }
        }

        // Proxy request to browser
        String responseBody = null;
        boolean exchangeAgain = false;
        int numRetries = 0;
        do {
            responseBody = exchange(httpEntity, requestContext, method,
                    sessionManager, optionalHttpEntity, isCreateSession);
            exchangeAgain = responseBody == null;
            if (this.isPostUrlRequest(method, requestContext)) {
                logger.debug("post Url is activated. webRtcActivated={}",
                        webRtcActivated);
                this.manageWebRtcMonitoring(sessionManager, webRtcActivated,
                        monitoringIndex);
            }
            if (exchangeAgain) {
                if (numRetries < createSessionRetries) {
                    logger.debug("Stopping browser and starting new one {}",
                            sessionManager);
                    stopBrowser(sessionManager);
                    sessionManager = startBrowser(newRequestBody, requestBody,
                            eusFilesService.getFilesPathInHostPath(),
                            sessionFolderPath, execData);
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
                sessionManager, liveSession, responseBody);

        if (isCreateSession) {
            // Maximize Browser Window
            String maximizeChrome = "/window/:windowHandle/maximize";
            String maximizeOther = "/window/maximize";
            try {
                exchange(httpEntity,
                        requestContext + "/" + sessionManager.getSessionId()
                                + maximizeChrome,
                        method, sessionManager, optionalHttpEntity, false);
            } catch (Exception e) {
                logger.error("Exception on window maximize with '{}' : {}",
                        maximizeChrome, e.getMessage());
                logger.debug("Trying maximize with '{}'", maximizeOther);

                try {
                    exchange(httpEntity,
                            requestContext + "/" + sessionManager.getSessionId()
                                    + maximizeOther,
                            method, sessionManager, optionalHttpEntity, false);
                } catch (Exception e1) {
                    logger.error("Exception on window maximize with '{}' too",
                            maximizeOther, e1);
                }
            }
            // Start Recording if not is manual recording
            if (!sessionManager.isManualRecording()) {
                // Start Recording
                logger.debug("Session with automatic recording");
                recordingService.startRecording(sessionManager);
            }
        }

        // Handle timeout
        handleTimeout(requestContext, method, sessionManager, isCreateSession,
                monitoringIndex);

        // Send Hub Container name too

        String jqSetHubContainerName = "walk(if type == \"object\" then .hubContainerName += \""
                + sessionManager.getHubContainerName() + "\"  else . end)";

        responseBody = jsonService.processJsonWithJq(responseBody,
                jqSetHubContainerName);

        return new ResponseEntity<>(responseBody, responseStatus);
    }

    public boolean interceptScriptIfIsNecessary(String requestBody,
            SessionManager sessionManager) {
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
                            recordingService.stopRecording(sessionManager);
                            recordingService.storeMetadata(sessionManager);

                            // If no testName, delete recording
                            if (sessionManager.getTestName() == null) {
                                try {
                                    Thread.sleep(1500);
                                } catch (Exception e) {
                                }
                                if (sessionManager.getFolderPath() == null) {

                                    recordingService.deleteRecording(
                                            sessionManager.getSessionId());
                                } else {
                                    recordingService.deleteRecording(
                                            sessionManager.getSessionId(),
                                            sessionManager.getFolderPath());
                                }
                            }
                            sessionService.sendRemoveSessionToAllClients(
                                    sessionManager);

                            // Set test name
                            sessionManager.setTestName((String) etScript
                                    .getArgs().get("testName"));

                            // Start recording
                            sessionService
                                    .sendNewSessionToAllClients(sessionManager);
                            recordingService.startRecording(sessionManager);
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

    public boolean manageWebRtcMonitoring(SessionManager sessionManager,
            boolean activated, String monitoringIndex) {
        boolean manageSuccessful = false;
        if (activated) {
            logger.debug("WebRtc monitoring activated");
            try {
                String configLocalStorageStr = this
                        .getWebRtcMonitoringLocalStorageStr(
                                sessionManager.getSessionId(), monitoringIndex);
                String postResponse = this.postScript(sessionManager,
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

    public String postScript(SessionManager sessionManager, String script,
            List<Object> args) throws JsonProcessingException, JSONException {
        String requestContext = webdriverSessionMessage + "/"
                + sessionManager.getSessionId() + webdriverExecuteScriptMessage;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject scriptObj = new JSONObject();
        scriptObj.put("script", script);
        scriptObj.put("args", args);
        String body = scriptObj.toString();

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        Optional<HttpEntity<String>> optionalHttpEntity = empty();
        return exchange(httpEntity, requestContext, POST, sessionManager,
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
            SessionManager sessionManager, boolean isCreateSession,
            String monitoringIndex) {
        // Browser log thread
        if (isCreateSession) {
            timeoutService.launchLogMonitor(sessionManager, monitoringIndex);
        }

        boolean disableTimeout = false;
        Integer timeout = Integer.parseInt(hubTimeout);
        if (sessionManager.getCapabilities() != null && sessionManager
                .getCapabilities().getElastestTimeout() != null) {
            timeout = sessionManager.getCapabilities().getElastestTimeout();
            if (timeout == 0) {
                disableTimeout = true;
                logger.debug("Timer disabled for session {}",
                        sessionManager.getSessionId());
            }
        }
        if (!disableTimeout) {
            logger.debug("Timer enabled for session {} with {} seconds",
                    sessionManager.getSessionId(), timeout);
            timeoutService.shutdownSessionTimer(sessionManager);
            final SessionManager finalSessionManager = sessionManager;
            Runnable deleteSession = () -> {
                deleteSession(finalSessionManager, true);
            };

            if (!isDeleteSessionRequest(method, requestContext)) {
                timeoutService.startSessionTimer(sessionManager, timeout,
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
            SessionManager sessionManager, boolean isLive, String responseBody)
            throws Exception {
        HttpStatus responseStatus = OK;

        // Intercept again create session
        if (isPostSessionRequest(method, requestContext)) {
            postSessionRequest(sessionManager, isLive, responseBody);
        }

        // Intercept destroy session
        if (isDeleteSessionRequest(method, requestContext)) {
            logger.trace("Intercepted DELETE session ({})", method);
            stopBrowser(sessionManager);
        }

        logger.debug("<< Response: {} -- body: {}", responseStatus,
                responseBody);
        return responseStatus;
    }

    private String exchange(HttpEntity<String> httpEntity,
            String requestContext, HttpMethod method,
            SessionManager sessionManager,
            Optional<HttpEntity<String>> optionalHttpEntity,
            boolean isCreateSession) throws JsonProcessingException {
        String hubUrl = sessionManager.getHubUrl();

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

    private void postSessionRequest(SessionManager sessionManager,
            boolean isLive, String responseBody)
            throws IOException, InterruptedException {
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
        sessionManager.setSessionId(sessionId);
        sessionManager.setLiveSession(isLive);

        sessionService.putSession(sessionId, sessionManager);
        sessionService.sendNewSessionToAllClients(sessionManager);
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

    public void deleteSession(SessionManager sessionManager, boolean timeout) {
        try {
            if (timeout) {
                logger.warn("Deleting session {} due to timeout of {} seconds",
                        sessionManager.getSessionId(),
                        sessionManager.getTimeout());
            } else {
                logger.info("Deleting session {}",
                        sessionManager.getSessionId());
            }

            if (sessionManager.getVncContainerName() != null) {
                // Stop recording even if manually managed
                recordingService.stopRecording(sessionManager);
                recordingService.storeMetadata(sessionManager);
                platformService.copyFilesFromBrowserIfNecessary(sessionManager,
                        null);
                sessionService.sendRecordingToAllClients(sessionManager);
            }

            sessionService.sendRemoveSessionToAllClients(sessionManager);

        } catch (Exception e) {
            logger.error("There was a problem deleting session {}",
                    sessionManager.getSessionId(), e);
            throw new EusException(e);
        } finally {
            try {
                sessionService.stopAllContainerOfSession(sessionManager);
            } catch (Exception e) {
                logger.debug("Containers of session {} not removed",
                        sessionManager.getSessionId());
            }
            sessionService.removeSession(sessionManager.getSessionId());

            timeoutService.shutdownSessionTimer(sessionManager);
        }
        if (timeout) {
            throw new EusException("Timeout of " + sessionManager.getTimeout()
                    + " seconds in session " + sessionManager.getSessionId());
        }
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

    public ResponseEntity<String> uploadFileToSession(String sessionId,
            MultipartFile file) throws IllegalStateException, IOException {
        Boolean saved = eusFilesService.uploadFileToSession(sessionId, file);

        if (saved) {
            return new ResponseEntity<>(file.getOriginalFilename(), OK);
        } else {

            return new ResponseEntity<>("Error on upload file: Already exists",
                    HttpStatus.CONFLICT);
        }
    }

    public ResponseEntity<String> uploadFileToSessionExecution(
            String executionKey, String sessionId, MultipartFile file)
            throws IllegalStateException, IOException {
        ExecutionData data = executionsMap.get(executionKey);
        Boolean saved = eusFilesService.uploadFileToSessionExecution(data,
                sessionId, file);

        if (saved) {
            return new ResponseEntity<>(file.getOriginalFilename(), OK);
        } else {

            return new ResponseEntity<>("Error on upload file: Already exists",
                    HttpStatus.CONFLICT);
        }
    }

    public InputStreamResource getFileFromBrowser(String sessionId,
            String filePath, Boolean isDirectory) throws Exception {
        SessionManager sessionManager;
        Optional<SessionManager> optionalSession = sessionService
                .getSession(sessionId);
        if (optionalSession.isPresent()) {
            sessionManager = optionalSession.get();
        } else {
            throw new Exception("Session " + sessionId + " not found");
        }

        InputStream fileStream = platformService
                .getFileFromBrowser(sessionManager, filePath, isDirectory);

        return new InputStreamResource(fileStream);
    }

    public String getSessionContextInfo(String sessionId) throws Exception {
        SessionManager sessionManager;
        Optional<SessionManager> optionalSession = sessionService
                .getSession(sessionId);
        if (optionalSession.isPresent()) {
            sessionManager = optionalSession.get();
        } else {
            throw new Exception("Session " + sessionId + " not found");
        }

        return platformService.getSessionContextInfo(sessionManager);

    }

    /* ************************************************** */
    /* ********* Manage Browser And EusServices ********* */
    /* ************************************************** */

    public SessionManager startBrowser(String requestBody,
            String originalRequestBody, String folderPath,
            String sessionFolderPath, ExecutionData execData) throws Exception {
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
        SessionManager sessionManager = new SessionManager(platformService);
        sessionManager.setElastestExecutionData(execData);
        sessionManager.setCapabilities(capabilities);
        sessionManager.addObserver(sessionService);

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
        sessionManager.setBrowser(browserName);
        sessionManager.setLiveSession(liveSession);
        sessionManager
                .setVersion(dockerHubService.getVersionFromImage(imageId));
        sessionManager.setFolderPath(sessionFolderPath);
        SimpleDateFormat dateFormat = new SimpleDateFormat(wsDateFormat);
        sessionManager.setCreationTime(dateFormat.format(new Date()));
        boolean manualRecording = jsonService
                .jsonToObject(originalRequestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().isManualRecording();
        sessionManager.setManualRecording(manualRecording);
        String testName = jsonService
                .jsonToObject(originalRequestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().getTestName();
        sessionManager.setTestName(testName);

        platformService.buildAndRunBrowserInContainer(sessionManager,
                eusContainerPrefix + hubContainerSufix, originalRequestBody,
                folderPath, execData, envs, labels, capabilities, imageId);

        sessionManager.buildHubUrl();
        String vncUrlFormat = "http://%s:%d/" + vncHtml
                + "?resize=scale&autoconnect=true&password=" + hubVncPassword;
        String vncUrl = format(vncUrlFormat, sessionManager.getHubIp(),
                sessionManager.getNoVncBindedPort());
        String internalVncUrl = vncUrl;
        logger.debug("HubUrl: {}", vncUrl);

        String etHost = getenv(etHostEnv);
        String etHostType = getenv(etHostEnvType);
        if (etHostType != null && etHost != null) {
            // If server-address and the platform is docker
            if (!"default".equalsIgnoreCase(etHostType)
                    && !etHost.equals("localhost")) {
                String hubIp = etHost;
                vncUrl = format(vncUrlFormat, hubIp,
                        sessionManager.getNoVncBindedPort());
            }
        }
        sessionManager.setVncUrl(vncUrl);
        platformService.waitForBrowserReady(
                sessionManager.getHubContainerName(), internalVncUrl,
                sessionManager);

        return sessionManager;
    }

    private void stopBrowser(SessionManager sessionManager) {
        deleteSession(sessionManager, false);
    }

    /* ********** Cross browser ********** */

    public BrowserSync startBrowsersyncService(ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities)
            throws Exception {
        Map<String, String> labels = new HashMap<>();
        labels.put(etTypeLabel, etTypeTSSLabelValue);
        labels.put(etTJobTssTypeLabel, "aux");

        if (execData != null) {
            labels.put(etTJobExecIdLabel, execData.gettJobExecId().toString());
            labels.put(etTJobIdLabel, execData.gettJobId().toString());
        }

        BrowserSync browserSync = platformService.buildAndRunBrowsersyncService(
                execData, crossBrowserCapabilities, labels);

        return browserSync;
    }

    public void stopBrowsersyncService(BrowserSync browserSync)
            throws Exception {
        String id = browserSync.getIdentifier();

        int killTimeoutInSeconds = 10;
        if (id != null && platformService.existServiceWithName(id)) {
            platformService.removeServiceWithTimeout(id, killTimeoutInSeconds);
        }
    }

    private CrossBrowserWebDriverCapabilities getCrossBrowserWebDriverCapabilities(
            HttpEntity<String> httpEntity) throws IOException {
        String requestBody = jsonService.sanitizeMessage(httpEntity.getBody());
        CrossBrowserWebDriverCapabilities crossBrowserCapabilities = jsonService
                .jsonToObject(requestBody,
                        CrossBrowserWebDriverCapabilities.class);
        return crossBrowserCapabilities;
    }

    public ResponseEntity<String> crossBrowserSession(
            HttpEntity<String> httpEntity, HttpServletRequest request)
            throws DockerException, Exception {
        return crossBrowserSession(httpEntity, request, null);
    }

    public ResponseEntity<String> crossBrowserSessionFromExecution(
            HttpEntity<String> httpEntity, HttpServletRequest request,
            String executionKey) throws DockerException, Exception {
        if (!executionsMap.containsKey(executionKey)) {
            logger.error(
                    "Cross Browser Request from Execution received but execution key {} not registered",
                    executionKey);
            return new ResponseEntity<>(executionKey, HttpStatus.BAD_REQUEST);
        }

        ExecutionData data = executionsMap.get(executionKey);
        logger.debug("Cross Browser Request from Execution {} received",
                data.gettJobExecId());
        return crossBrowserSession(httpEntity, request, data);
    }

    public ResponseEntity<String> crossBrowserSession(
            HttpEntity<String> httpEntity, HttpServletRequest request,
            ExecutionData data) throws DockerException, Exception {
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        String requestContext = getRequestContext(request);

        if (data != null) {
            requestContext = parseRequestContext(requestContext);
        }

        String requestBody = jsonService.sanitizeMessage(httpEntity.getBody());
        logger.debug(">> CrossBrowser Request: {} {} -- body: {}", method,
                requestContext, requestBody);

        switch (method) {
        case POST:
            return startCrossBrowserSession(httpEntity, request, requestContext,
                    data);
        case DELETE:
            return stopCrossBrowserSession(requestContext);
        case GET:
            return getCrossBrowserSession(requestContext);
        default:
            return new ResponseEntity<String>("Method not allowed",
                    HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<String> startCrossBrowserSession(
            HttpEntity<String> httpEntity, HttpServletRequest request,
            String requestContext, ExecutionData data)
            throws IOException, Exception, JsonProcessingException,
            DockerException, JsonParseException, JsonMappingException {
        CrossBrowserWebDriverCapabilities crossBrowserCapabilities = getCrossBrowserWebDriverCapabilities(
                httpEntity);

        BrowserSync browserSync = startBrowsersyncService(null,
                crossBrowserCapabilities);

        String sessionRequestContext = getRequestContextWithoutCrossBrowser(
                requestContext);

        // Start each sessions
        for (WebDriverCapabilities sessionCapabilities : crossBrowserCapabilities
                .getSessionsCapabilities()) {
            String currentRequestBody = jsonService
                    .objectToJson(sessionCapabilities);
            ResponseEntity<String> response;

            // From Execution
            if (data != null) {
                response = sessionFromExecution(httpEntity,
                        sessionRequestContext, currentRequestBody,
                        request.getMethod(), data);
            } else { // Normal session
                response = session(httpEntity, sessionRequestContext,
                        currentRequestBody, request.getMethod());
            }

            Map<String, Object> responseMap = jsonService.jsonToObject(
                    response.getBody(),
                    new TypeReference<Map<String, Object>>() {
                    });
            if (responseMap != null) {
                String sessionId = (String) responseMap.get("sessionId");
                if (sessionId == null && responseMap.get("value") != null) {
                    LinkedHashMap<String, Object> value = (LinkedHashMap<String, Object>) responseMap
                            .get("value");
                    sessionId = (String) value.get("sessionId");
                }

                if (sessionId != null) {
                    // Get Session Manager and save in BrowserSync
                    Optional<SessionManager> optionalSessionManager = sessionService
                            .getSession(sessionId);
                    SessionManager sessionManager = optionalSessionManager
                            .get();
                    if (sessionManager != null) {
                        browserSync.getSessions().add(sessionManager);

                        // Open app url in browser
                        String getUrlContext = sessionRequestContext + "/"
                                + sessionId + "/url";
                        String getUrlRequestBody = "{ \"url\": \""
                                + browserSync.getAppUrl() + "\" }";

                        Optional<HttpEntity<String>> optionalHttpEntity = Optional
                                .of(new HttpEntity<String>(getUrlRequestBody,
                                        httpEntity.getHeaders()));
                        try {
                            String responseBody = exchange(httpEntity,
                                    getUrlContext, POST, sessionManager,
                                    optionalHttpEntity, false);
                        } catch (Exception e) {
                            logger.error(
                                    "Error on navigate to url in Crossbrowser session {}: {}",
                                    sessionId, e.getMessage());
                        }
                    } else {
                        logger.error(
                                "Crossbrowser session Error: Session Manager is null");
                    }
                } else {
                    logger.error(
                            "Crossbrowser session Error: Session ID is null");
                }

            }
        }

        crossBrowserRegistry.put(browserSync.getIdentifier(), browserSync);

        return new ResponseEntity<String>(jsonService.objectToJson(browserSync),
                OK);
    }

    private ResponseEntity<String> stopCrossBrowserSession(
            String requestContext)
            throws IOException, Exception, JsonProcessingException,
            DockerException, JsonParseException, JsonMappingException {

        Optional<String> crossBrowserIdFromPath = getCrossBrowserIdFromPath(
                requestContext);
        if (crossBrowserIdFromPath.isPresent()) {
            String crossBrowserId = crossBrowserIdFromPath.get();
            return stopCrossBrowserSessionById(crossBrowserId);
        } else {
            return notFound();
        }

    }

    private ResponseEntity<String> stopCrossBrowserSessionById(
            String crossBrowserId) throws Exception {
        if (crossBrowserRegistry.containsKey(crossBrowserId)) {
            BrowserSync browserSync = crossBrowserRegistry.get(crossBrowserId);
            if (browserSync != null) {
                // Stop all sessions
                for (SessionManager sessionManager : browserSync
                        .getSessions()) {
                    deleteSession(sessionManager, false);
                }

                stopBrowsersyncService(browserSync);

                crossBrowserRegistry.remove(crossBrowserId);
                return new ResponseEntity<>("Crossbrowser with id "
                        + crossBrowserId + " has been stopper", OK);
            } else {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    public void stopCrossBrowserSessions() {
        for (ConcurrentHashMap.Entry<String, BrowserSync> crossBrowser : crossBrowserRegistry
                .entrySet()) {
            String identifier = crossBrowser.getValue().getIdentifier();
            try {
                stopCrossBrowserSessionById(identifier);
            } catch (Exception e) {
                logger.error("Error on stop cross browser session with id {}",
                        identifier);
            }
        }
    }

    private ResponseEntity<String> getCrossBrowserSession(String requestContext)
            throws IOException, Exception, JsonProcessingException,
            DockerException, JsonParseException, JsonMappingException {

        Optional<String> crossBrowserIdFromPath = getCrossBrowserIdFromPath(
                requestContext);
        if (crossBrowserIdFromPath.isPresent()) {
            String crossBrowserId = crossBrowserIdFromPath.get();
            logger.debug("Getting crossbrowser data with id {}",
                    crossBrowserId);
            if (crossBrowserRegistry.containsKey(crossBrowserId)) {
                BrowserSync browserSync = crossBrowserRegistry
                        .get(crossBrowserId);
                return new ResponseEntity<String>(
                        jsonService.objectToJson(browserSync), OK);
            }
        } else {
            return notFound();
        }
        return notFound();
    }

    public Optional<String> getCrossBrowserIdFromPath(String path) {
        Optional<String> out = Optional.empty();
        int i = path.indexOf(webdriverCrossbrowserSessionMessage);

        if (i != -1) {
            int j = path.indexOf('/',
                    i + webdriverCrossbrowserSessionMessage.length());
            if (j != -1) {
                int k = path.indexOf('/', j + 1);
                int cut = (k == -1) ? path.length() : k;

                String sessionId = path.substring(j + 1, cut);
                out = Optional.of(sessionId);
            }
        }

        logger.trace("getCrossBrowserIdFromPath -- path: {} crossbrowser id {}",
                path, out);

        return out;
    }

    public String getRequestContextWithoutCrossBrowser(String contextPath) {
        String newContextPath = "";
        try {
            // If context path == .../crossbrowser/...
            newContextPath += contextPath.split("/crossbrowser")[0];
        } catch (Exception e) {
        }

        return newContextPath + "/session";
    }

}
