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

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger log = LoggerFactory
            .getLogger(EusController.class);

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

        // TODO implementation

        return new ResponseEntity<Void>(OK);
    }

    public ResponseEntity<AudioLevel> getAudioLevel(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId) {
        log.debug("[getAudioLevel] sessionId={} elementId={}", sessionId,
                elementId);

        // TODO implementation

        return new ResponseEntity<AudioLevel>(OK);
    }

    public ResponseEntity<ColorValue> getColorByCoordinates(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Coordinate in x-axis", defaultValue = "0") @RequestParam(value = "x", required = false, defaultValue = "0") Integer x,
            @ApiParam(value = "Coordinate in y-axis", defaultValue = "0") @RequestParam(value = "y", required = false, defaultValue = "0") Integer y) {
        log.debug("[getColorByCoordinates] sessionId={} elementId={} x={} y={}",
                sessionId, elementId, x, y);

        // TODO implementation

        return new ResponseEntity<ColorValue>(OK);
    }

    public ResponseEntity<List<StatsValue>> getStats(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId) {
        log.debug("[getStats] sessionId={} subscriptionId={}", sessionId,
                subscriptionId);

        // TODO implementation

        return new ResponseEntity<List<StatsValue>>(OK);
    }

    public ResponseEntity<EventValue> getSubscriptionValue(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Identifier of peerconnection") @RequestParam(value = "peerconnectionId", required = false) String peerconnectionId) {
        log.debug("[getSubscriptionValue] sessionId={} peerconnectionId={}",
                sessionId, peerconnectionId);

        // TODO implementation

        return new ResponseEntity<EventValue>(OK);
    }

    public ResponseEntity<Void> setUserMedia(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Media URL to take WebRTC user media", required = true) @RequestBody UserMedia body) {
        log.debug("[setUserMedia] sessionId={} userMedia={}", sessionId, body);

        // TODO implementation

        return new ResponseEntity<Void>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToEvent(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter) and sample rate (in ms)", required = true) @RequestBody Latency body) {
        log.debug("[subscribeToEvent] sessionId={} elementId={} latency={}",
                sessionId, elementId, body);

        // TODO implementation

        return new ResponseEntity<EventSubscription>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToLatency(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter), selection of QoE algorithm, and sample rate (in ms)", required = true) @RequestBody Quality body) {
        log.debug("[subscribeToLatency] sessionId={} elementId={} quality={}",
                sessionId, elementId, body);

        // TODO implementation

        return new ResponseEntity<EventSubscription>(OK);
    }

    public ResponseEntity<EventSubscription> subscribeToQuality(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Event name to be subscribed", required = true) @RequestBody Event body) {
        log.debug("[subscribeToQuality] sessionId={} elementId={} event={}",
                sessionId, elementId, body);

        // TODO implementation

        return new ResponseEntity<EventSubscription>(OK);
    }

    @Override
    public ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request) {
        return webDriverService.session(httpEntity, request);
    }

    @Override
    public ResponseEntity<String> getStatus() {
        return webDriverService.getStatus();
    }

    @Override
    public ResponseEntity<String> vnc(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId) {
        return vncService.getVnc(sessionId);

    }

    @Override
    public ResponseEntity<String> recording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            HttpServletRequest request) {
        ResponseEntity<String> response = null;
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        if (method == GET) {
            response = recordingService.getRecording(sessionId);
        } else if (method == DELETE) {
            response = recordingService.deleteRecording(sessionId);
        }
        return response;
    }

}
