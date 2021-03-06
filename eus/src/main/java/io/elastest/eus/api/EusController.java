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
package io.elastest.eus.api;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.elastest.epm.client.service.JsonService;
import io.elastest.eus.api.model.AudioLevel;
import io.elastest.eus.api.model.ColorValue;
import io.elastest.eus.api.model.Event;
import io.elastest.eus.api.model.EventSubscription;
import io.elastest.eus.api.model.EventValue;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.api.model.Latency;
import io.elastest.eus.api.model.Quality;
import io.elastest.eus.api.model.StatsValue;
import io.elastest.eus.api.model.UserMedia;
import io.elastest.eus.json.VideoTimeInfo;
import io.elastest.eus.service.DockerHubService;
import io.elastest.eus.service.RecordingService;
import io.elastest.eus.service.SessionService;
import io.elastest.eus.service.VncService;
import io.elastest.eus.service.WebDriverService;
import io.elastest.eus.session.SessionManager;
import io.swagger.annotations.ApiParam;

/**
 * API controller.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Controller
@CrossOrigin
public class EusController implements EusApi {

    final Logger log = getLogger(lookup().lookupClass());

    private WebDriverService webDriverService;
    private VncService vncService;
    private RecordingService recordingService;
    private DockerHubService dockerHubService;
    private JsonService jsonService;
    private SessionService sessionService;

    @Autowired
    public EusController(WebDriverService webDriverService, VncService vncService,
            RecordingService recordingService, DockerHubService dockerHubService,
            JsonService jsonService, SessionService sessionService) {
        this.webDriverService = webDriverService;
        this.vncService = vncService;
        this.recordingService = recordingService;
        this.dockerHubService = dockerHubService;
        this.jsonService = jsonService;
        this.sessionService = sessionService;
    }

    public ResponseEntity<Void> deleteSubscription(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId) {
        log.debug("[deleteSubscription] sessionId={} subscriptionId={}", sessionId, subscriptionId);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<AudioLevel> getAudioLevel(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId) {
        log.debug("[getAudioLevel] sessionId={} elementId={}", sessionId, elementId);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<ColorValue> getColorByCoordinates(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Coordinate in x-axis", defaultValue = "0") @RequestParam(value = "x", required = false, defaultValue = "0") Integer x,
            @ApiParam(value = "Coordinate in y-axis", defaultValue = "0") @RequestParam(value = "y", required = false, defaultValue = "0") Integer y) {
        log.debug("[getColorByCoordinates] sessionId={} elementId={} x={} y={}", sessionId,
                elementId, x, y);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<List<StatsValue>> getStats(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId) {
        log.debug("[getStats] sessionId={} subscriptionId={}", sessionId, subscriptionId);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventValue> getSubscriptionValue(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Identifier of peerconnection") @RequestParam(value = "peerconnectionId", required = false) String peerconnectionId) {
        log.debug("[getSubscriptionValue] sessionId={} peerconnectionId={}", sessionId,
                peerconnectionId);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<Void> setUserMedia(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Media URL to take WebRTC user media", required = true) @RequestBody UserMedia body) {
        log.debug("[setUserMedia] sessionId={} userMedia={}", sessionId, body);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToEvent(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter) and sample rate (in ms)", required = true) @RequestBody Latency body) {
        log.debug("[subscribeToEvent] sessionId={} elementId={} latency={}", sessionId, elementId,
                body);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToLatency(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter), selection of QoE algorithm, and sample rate (in ms)", required = true) @RequestBody Quality body) {
        log.debug("[subscribeToLatency] sessionId={} elementId={} quality={}", sessionId, elementId,
                body);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToQuality(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Event name to be subscribed", required = true) @RequestBody Event body) {
        log.debug("[subscribeToQuality] sessionId={} elementId={} event={}", sessionId, elementId,
                body);

        // TODO Not implemented yet

        return new ResponseEntity<>(OK);
    }

    /* *************************** */
    /* ********* Session ********* */
    /* *************************** */

    @Override
    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.session(httpEntity, request);
        } catch (Exception e) {
            log.error("Exception handling session {}", request, e);
            response = webDriverService
                    .getErrorResponse("Exception handling session: " + e.getMessage(), e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> sessionFromExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            HttpEntity<String> httpEntity, HttpServletRequest request) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.sessionFromExecution(httpEntity, request, key);
        } catch (Exception e) {
            log.error("Exception handling session {}", request, e);
            response = webDriverService
                    .getErrorResponse("Exception handling session: " + e.getMessage(), e);
        }
        return response;
    }

    /* ************************************************* */
    /* ********* Register/Unregister execution ********* */
    /* ************************************************* */

    @Override
    public ResponseEntity<String> registerExecution(
            @ApiParam(value = "The Execution Data", required = true) @Valid @RequestBody ExecutionData executionData) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.registerExecution(executionData);
        } catch (Exception e) {
            log.error("Exception registering execution {}", executionData, e);
            response = webDriverService.getErrorResponse("Exception registering execution", e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> unregisterExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.unregisterExecution(key);
        } catch (Exception e) {
            log.error("Exception unregistering execution {}", key, e);
            response = webDriverService.getErrorResponse("Exception registering execution", e);
        }
        return response;
    }

    /* ************************** */
    /* ********* Status ********* */
    /* ************************** */

    @Override
    public ResponseEntity<String> getStatus() {
        ResponseEntity<String> response;
        try {
            response = webDriverService.getStatus();
        } catch (Exception e) {
            response = webDriverService.getErrorResponse("Exception getting status", e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> getStatusExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key) {
        return this.getStatus();
    }

    /* ************************** */
    /* ******** Browsers ******** */
    /* ************************** */

    public ResponseEntity<String> getBrowsers(boolean cached) {
        try {
            return new ResponseEntity<String>(
                    jsonService.objectToJson(dockerHubService.getBrowsers(cached)), OK);
        } catch (IOException e) {
            return webDriverService.getErrorResponse("Exception getting browsers", e);
        }
    }

    @Override
    public ResponseEntity<String> getBrowsers() {
        return getBrowsers(false);
    }

    @Override
    public ResponseEntity<String> getCachedBrowsers() {
        return getBrowsers(true);
    }

    /* ************************* */
    /* ********** Vnc ********** */
    /* ************************* */

    @Override
    public ResponseEntity<String> vnc(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId) {
        ResponseEntity<String> response;
        try {
            response = vncService.getVnc(sessionId);
        } catch (Exception e) {
            response = webDriverService.getErrorResponse("Exception getting VNC session", e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> executionVnc(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key) {
        return this.vnc(sessionId);
    }

    /* *************************** */
    /* ******** Recording ******** */
    /* *************************** */

    @Override
    public ResponseEntity<String> recording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            HttpServletRequest request) {
        ResponseEntity<String> response;
        try {
            HttpMethod method = HttpMethod.resolve(request.getMethod());
            if (method == GET) {
                response = recordingService.getRecording(sessionId);
            } else {
                // The only option here is DELETE method
                response = recordingService.deleteRecording(sessionId);
            }
        } catch (Exception e) {
            response = webDriverService
                    .getErrorResponse("Exception handling recording in session " + sessionId, e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> executionRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            HttpServletRequest request,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key) {
        ResponseEntity<String> response;
        try {
            HttpMethod method = HttpMethod.resolve(request.getMethod());
            ExecutionData eusData = webDriverService.getExecutionsMap().get(key);
            String filePath = webDriverService.eusFilesService
                    .getHostSessionFolderFromExecution(eusData);
            if (method == GET) {
                response = recordingService.getRecording(sessionId, filePath);
            } else {
                // The only option here is DELETE method
                response = recordingService.deleteRecording(sessionId, filePath);
            }
        } catch (Exception e) {
            response = webDriverService
                    .getErrorResponse("Exception handling recording in session " + sessionId, e);
        }
        return response;
    }

    /* ***************************** */
    /* ****** Start Recording ****** */
    /* ***************************** */

    @Override
    public ResponseEntity<String> startRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName,
            @ApiParam(value = "The Video Name", required = true) @Valid @RequestBody String videoName) {
        try {
            Optional<SessionManager> sessionManager = sessionService.getSession(sessionId);
            recordingService.startRecording(sessionManager.get(), hubContainerName, videoName);
            return new ResponseEntity<String>(OK);

        } catch (Exception e) {
            ResponseEntity<String> response = webDriverService
                    .getErrorResponse("Exception starting recording with name " + videoName
                            + " in session " + sessionId, e);
            return response;
        }
    }

    @Override
    public ResponseEntity<String> startExecutionRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName,
            @ApiParam(value = "The Video Name", required = true) @Valid @RequestBody String videoName,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key) {
        return this.startRecording(sessionId, hubContainerName, videoName);
    }

    /* *************************** */
    /* ****** Stop Recording****** */
    /* *************************** */

    @Override
    public ResponseEntity<String> stopRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName) {
        try {
            Optional<SessionManager> sessionManager = sessionService.getSession(sessionId);
            recordingService.stopRecording(sessionManager.get(), hubContainerName);
            return new ResponseEntity<String>(OK);

        } catch (Exception e) {
            ResponseEntity<String> response = webDriverService
                    .getErrorResponse("Exception stopping recording in session " + sessionId, e);
            return response;
        }
    }

    @Override
    public ResponseEntity<String> stopExecutionRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            HttpServletRequest request) {
        return this.stopRecording(sessionId, hubContainerName);
    }

    @Override
    public ResponseEntity<String> stopRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId) {
        try {
            Optional<SessionManager> sessionManager = sessionService.getSession(sessionId);
            recordingService.stopRecording(sessionManager.get());
            return new ResponseEntity<String>(OK);

        } catch (Exception e) {
            ResponseEntity<String> response = webDriverService
                    .getErrorResponse("Exception stopping recording in session " + sessionId, e);
            return response;
        }
    }

    @Override
    public ResponseEntity<String> stopExecutionRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            HttpServletRequest request) {
        return this.stopRecording(sessionId);
    }

    /* *************************** */
    /* ********** Files ********** */
    /* *************************** */

    @Override
    public ResponseEntity<InputStreamResource> getFile(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @RequestParam(value = "isDirectory", required = false) Boolean isDirectory,
            HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String[] splittedRequestURL = requestURL.split(sessionId);
        String filePath = "";
        if (splittedRequestURL.length == 2) {
            filePath = splittedRequestURL[1];
        } else if (splittedRequestURL.length > 2) {
            // This case its only if file contains sessionId in its name
            boolean isFirst = true;
            for (String part : splittedRequestURL) {
                if (!isFirst) {
                    if (filePath != "") {
                        filePath += sessionId;
                    }
                    filePath += part;
                }
                isFirst = false;
            }
        }

        if (isDirectory == null) {
            isDirectory = false;
        }

        try {
            InputStreamResource resource = webDriverService.getFileFromBrowser(sessionId, filePath,
                    isDirectory);

            // Try to determine file's content type
            String contentType = request.getServletContext().getMimeType(filePath);

            String fileName;

            if (isDirectory || contentType == null || contentType.isEmpty()) {
                // Is folder => Tar file with file(s) into
                contentType = "application/x-tar";
                fileName = "download_from_session_" + sessionId + ".tar";
            } else {
                Path p = Paths.get(filePath);
                fileName = p.getFileName().toString();
            }

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Exception on get file {} from session {}", filePath, sessionId, e);
            try {
                log.debug("Session Info: {}", webDriverService.getSessionContextInfo(sessionId));
            } catch (Exception e1) {
                log.error("Error on get Session {} Info", sessionId, e1.getMessage());
            }
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<InputStreamResource> executionGetFile(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            @RequestParam(value = "isDirectory", required = false) Boolean isDirectory,
            HttpServletRequest request) {
        return getFile(sessionId, isDirectory, request);
    }

    @Override
    public ResponseEntity<String> uploadFileToSession(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path) {
        return uploadFileToSessionExecution(sessionId, null, file, path);
    }

    @Override
    public ResponseEntity<String> uploadFileToSessionExecution(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.uploadFileToSession(key, sessionId, path, file);
        } catch (Exception e) {
            log.error("Exception on upload file to session {}", sessionId, e);
            response = webDriverService.getErrorResponse("Exception on upload file to session", e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> uploadFileToSessionFromUrl(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @RequestParam(value = "fileUrl") String fileUrl,
            @RequestParam(value = "fileName") String fileName,
            @RequestParam(value = "path", required = false) String path) {
        return uploadFileToSessionExecutionFromUrl(sessionId, null, fileUrl, fileName, path);
    }

    @Override
    public ResponseEntity<String> uploadFileToSessionExecutionFromUrl(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            @RequestParam(value = "fileUrl") String fileUrl,
            @RequestParam(value = "fileName") String fileName,
            @RequestParam(value = "path", required = false) String path) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.uploadFileToSessionFromUrl(key, sessionId, path, fileUrl,
                    fileName);
        } catch (Exception e) {
            log.error("Exception on upload file to session {} from url {}", sessionId, fileUrl, e);
            response = webDriverService
                    .getErrorResponse("Exception on upload file from url to session", e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> crossBrowserSession(HttpEntity<String> httpEntity,
            HttpServletRequest request) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.crossBrowserSession(httpEntity, request);
        } catch (Exception e) {
            log.error("Exception handling Crossbrowser session {}", request, e);
            response = webDriverService.getErrorResponse("Exception handling Crossbrowser session",
                    e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> executionCrossBrowserSession(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            HttpEntity<String> httpEntity, HttpServletRequest request) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.crossBrowserSessionFromExecution(httpEntity, request, key);
        } catch (Exception e) {
            log.error("Exception handling Crossbrowser session {}", request, e);
            response = webDriverService.getErrorResponse("Exception handling Crossbrowser session",
                    e);
        }
        return response;
    }

    /* *************************************** */
    /* ************* QoE metrics ************* */
    /* *************************************** */

    public ResponseEntity<String> startWebRTCQoEMeter(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @RequestParam(value = "presenterPath", required = true) String presenterCompleteFilePath,
            @RequestParam(value = "presenterSessionId", required = true) String presenterSessionId,
            @RequestParam(value = "viewerPath", required = true) String viewerCompleteFilePath,
            @RequestParam(value = "viewerSessionId", required = true) String viewerSessionId) {
        try {
            SessionManager presenterSessionManager = sessionService.getSession(presenterSessionId)
                    .get();
            SessionManager viewerSessionManager = sessionService.getSession(viewerSessionId).get();

            SessionManager webRTCQoESessionManager = presenterSessionManager;

            // Start service
            String identifier = webDriverService.qoeService.startService(webRTCQoESessionManager);

            // Download and upload
            webDriverService.qoeService.downloadVideosFromBrowserAndUploadToQoE(
                    webRTCQoESessionManager, presenterSessionManager, viewerSessionManager,
                    identifier, presenterCompleteFilePath, viewerCompleteFilePath);

            // Async calculate
            webDriverService.qoeService.calculateQoEMetricsAsync(webRTCQoESessionManager,
                    identifier);

            return new ResponseEntity<String>(identifier, OK);

        } catch (Exception e) {
            ResponseEntity<String> response = webDriverService.getErrorResponse(
                    "Exception starting WebRTC QoE Meter with in session " + sessionId, e);
            return response;
        }
    }

    public ResponseEntity<String> executionStartWebRTCQoEMeter(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @RequestParam(value = "presenterPath", required = true) String presenterCompleteFilePath,
            @RequestParam(value = "presenterSessionId", required = true) String presenterSessionId,
            @RequestParam(value = "viewerPath", required = true) String viewerCompleteFilePath,
            @RequestParam(value = "viewerSessionId", required = true) String viewerSessionId) {
        return this.startWebRTCQoEMeter(sessionId, presenterCompleteFilePath, presenterSessionId,
                viewerCompleteFilePath, viewerSessionId);
    }

    public ResponseEntity<Boolean> isWebRTCQoEMeterCsvGenerated(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier)
            throws Exception {
        Boolean generated = webDriverService.qoeService.isCsvAlreadyGenerated(identifier);
        return new ResponseEntity<Boolean>(generated, OK);
    }

    public ResponseEntity<Boolean> executionIsWebRTCQoEMeterCsvGenerated(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier)
            throws Exception {
        return this.isWebRTCQoEMeterCsvGenerated(sessionId, identifier);
    }

    public ResponseEntity<Map<String, byte[]>> getWebRTCQoEMeterCsv(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier)
            throws Exception {
        SessionManager webRTCQoESessionManager = sessionService.getSession(sessionId).get();

        Map<String, byte[]> csvMap = webDriverService.qoeService
                .getQoEMetricsCSV(webRTCQoESessionManager, identifier);

        return new ResponseEntity<Map<String, byte[]>>(csvMap, OK);
    }

    public ResponseEntity<Map<String, byte[]>> executionGetWebRTCQoEMeterCsv(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier)
            throws Exception {
        return this.getWebRTCQoEMeterCsv(sessionId, identifier);
    }

    public ResponseEntity<Map<String, Double>> getWebRTCQoEMeterMetric(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier)
            throws Exception {
        SessionManager sessionManager = sessionService.getSession(sessionId).get();

        return new ResponseEntity<Map<String, Double>>(
                webDriverService.qoeService.getQoEAverageMetrics(sessionManager, identifier, false),
                OK);
    }

    public ResponseEntity<Map<String, Double>> executionGetWebRTCQoEMeterMetric(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier)
            throws Exception {
        return getWebRTCQoEMeterMetric(sessionId, identifier);
    }

    public ResponseEntity<String> postWebRTCQoEMeterMetricsTime(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier,
            @ApiParam(value = "Object that stores start time and video duration", required = true) @RequestBody VideoTimeInfo body)
            throws Exception {
        SessionManager sessionManager = sessionService.getSession(sessionId).get();

        webDriverService.qoeService.assignTimeToQoEMetrics(sessionManager, identifier, body);

        return new ResponseEntity<String>("{}", OK);

    }

    public ResponseEntity<String> executionPostWebRTCQoEMeterMetricsTime(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier,
            @ApiParam(value = "Object that stores start time and video duration", required = true) @RequestBody VideoTimeInfo body)
            throws Exception {
        return postWebRTCQoEMeterMetricsTime(sessionId, identifier, body);

    }

    public ResponseEntity<String> createWebRTCQoEMeterService(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId)
            throws Exception {
        SessionManager sessionManager = sessionService.getSession(sessionId).get();
        String identifier = webDriverService.qoeService.createService(sessionManager);
        return new ResponseEntity<String>(identifier, OK);
    }

    public ResponseEntity<String> executionCreateWebRTCQoEMeterService(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId)
            throws Exception {
        return createWebRTCQoEMeterService(sessionId);
    }

    public ResponseEntity<String> uploadCsvToQoESession(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier,
            @RequestParam(value = "fileUrl") String fileUrl,
            @RequestParam(value = "fileName") String fileName) throws Exception {
        SessionManager sessionManager = sessionService.getSession(sessionId).get();
        return webDriverService.qoeService.uploadCsvFromUrlToQoEFolder(sessionManager, identifier,
                fileUrl, fileName);
    }

    public ResponseEntity<String> uploadCsvToQoESessionExecution(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "QoE Service identifier (previously established)", required = true) @PathVariable("identifier") String identifier,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            @RequestParam(value = "fileUrl") String fileUrl,
            @RequestParam(value = "fileName") String fileName) throws Exception {
        return uploadCsvToQoESession(sessionId, identifier, fileUrl, fileName);
    }

}
