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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
import io.elastest.eus.service.RecordingService;
import io.elastest.eus.service.VncService;
import io.elastest.eus.service.WebDriverService;
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

    @Autowired
    public EusController(WebDriverService webDriverService,
            VncService vncService, RecordingService recordingService) {
        this.webDriverService = webDriverService;
        this.vncService = vncService;
        this.recordingService = recordingService;
    }

    public ResponseEntity<Void> deleteSubscription(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId) {
        log.debug("[deleteSubscription] sessionId={} subscriptionId={}",
                sessionId, subscriptionId);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<AudioLevel> getAudioLevel(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId) {
        log.debug("[getAudioLevel] sessionId={} elementId={}", sessionId,
                elementId);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<ColorValue> getColorByCoordinates(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Coordinate in x-axis", defaultValue = "0") @RequestParam(value = "x", required = false, defaultValue = "0") Integer x,
            @ApiParam(value = "Coordinate in y-axis", defaultValue = "0") @RequestParam(value = "y", required = false, defaultValue = "0") Integer y) {
        log.debug("[getColorByCoordinates] sessionId={} elementId={} x={} y={}",
                sessionId, elementId, x, y);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<List<StatsValue>> getStats(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId) {
        log.debug("[getStats] sessionId={} subscriptionId={}", sessionId,
                subscriptionId);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventValue> getSubscriptionValue(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Identifier of peerconnection") @RequestParam(value = "peerconnectionId", required = false) String peerconnectionId) {
        log.debug("[getSubscriptionValue] sessionId={} peerconnectionId={}",
                sessionId, peerconnectionId);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<Void> setUserMedia(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Media URL to take WebRTC user media", required = true) @RequestBody UserMedia body) {
        log.debug("[setUserMedia] sessionId={} userMedia={}", sessionId, body);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToEvent(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter) and sample rate (in ms)", required = true) @RequestBody Latency body) {
        log.debug("[subscribeToEvent] sessionId={} elementId={} latency={}",
                sessionId, elementId, body);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToLatency(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter), selection of QoE algorithm, and sample rate (in ms)", required = true) @RequestBody Quality body) {
        log.debug("[subscribeToLatency] sessionId={} elementId={} quality={}",
                sessionId, elementId, body);

        // Not implemented yet

        return new ResponseEntity<>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToQuality(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Event name to be subscribed", required = true) @RequestBody Event body) {
        log.debug("[subscribeToQuality] sessionId={} elementId={} event={}",
                sessionId, elementId, body);

        // Not implemented yet

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
                    .getErrorResponse("Exception handling session", e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> sessionFromExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            HttpEntity<String> httpEntity, HttpServletRequest request) {
        ResponseEntity<String> response;
        try {
            response = webDriverService.sessionFromExecution(httpEntity,
                    request, key);
        } catch (Exception e) {
            log.error("Exception handling session {}", request, e);
            response = webDriverService
                    .getErrorResponse("Exception handling session", e);
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
            response = webDriverService
                    .getErrorResponse("Exception registering execution", e);
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
            response = webDriverService
                    .getErrorResponse("Exception registering execution", e);
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
            response = webDriverService
                    .getErrorResponse("Exception getting status", e);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> getStatusExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key) {
        return this.getStatus();
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
            response = webDriverService
                    .getErrorResponse("Exception getting VNC session", e);
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
            response = webDriverService.getErrorResponse(
                    "Exception handling recording in session " + sessionId, e);
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
            if (method == GET) {
                response = recordingService.getRecording(sessionId,
                        webDriverService.getExecutionsMap().get(key)
                                .getFolderPath());
            } else {
                // The only option here is DELETE method
                response = recordingService.deleteRecording(sessionId,
                        webDriverService.getExecutionsMap().get(key)
                                .getFolderPath());
            }
        } catch (Exception e) {
            response = webDriverService.getErrorResponse(
                    "Exception handling recording in session " + sessionId, e);
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
            recordingService.startRecording(sessionId, hubContainerName,
                    videoName);
            return new ResponseEntity<String>(OK);

        } catch (Exception e) {
            ResponseEntity<String> response = webDriverService
                    .getErrorResponse(
                            "Exception starting recording with name "
                                    + videoName + " in session " + sessionId,
                            e);
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
            recordingService.stopRecording(hubContainerName);
            return new ResponseEntity<String>(OK);

        } catch (Exception e) {
            ResponseEntity<String> response = webDriverService.getErrorResponse(
                    "Exception stopping recording in session " + sessionId, e);
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
}
