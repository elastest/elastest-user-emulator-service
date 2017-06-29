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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import io.elastest.eus.api.model.AudioLevel;
import io.elastest.eus.api.model.ColorValue;
import io.elastest.eus.api.model.Event;
import io.elastest.eus.api.model.EventSubscription;
import io.elastest.eus.api.model.EventValue;
import io.elastest.eus.api.model.Latency;
import io.elastest.eus.api.model.Quality;
import io.elastest.eus.api.model.StatsValue;
import io.elastest.eus.api.model.UserMedia;
import io.swagger.annotations.ApiParam;

/**
 * API controller.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Controller
public class SessionApiController implements SessionApi {

    private final Logger log = LoggerFactory
            .getLogger(SessionApiController.class);

    @Value("${server.contextPath}")
    private String contextPath;

    public ResponseEntity<Void> deleteSubscription(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId) {
        log.debug("[deleteSubscription] sessionId={} subscriptionId={}",
                sessionId, subscriptionId);

        // TODO implementation

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<AudioLevel> getAudioLevel(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId) {
        log.debug("[getAudioLevel] sessionId={} elementId={}", sessionId,
                elementId);

        // TODO implementation

        return new ResponseEntity<AudioLevel>(HttpStatus.OK);
    }

    public ResponseEntity<ColorValue> getColorByCoordinates(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Coordinate in x-axis", defaultValue = "0") @RequestParam(value = "x", required = false, defaultValue = "0") Integer x,
            @ApiParam(value = "Coordinate in y-axis", defaultValue = "0") @RequestParam(value = "y", required = false, defaultValue = "0") Integer y) {
        log.debug("[getColorByCoordinates] sessionId={} elementId={} x={} y={}",
                sessionId, elementId, x, y);

        // TODO implementation

        return new ResponseEntity<ColorValue>(HttpStatus.OK);
    }

    public ResponseEntity<List<StatsValue>> getStats(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId) {
        log.debug("[getStats] sessionId={} subscriptionId={}", sessionId,
                subscriptionId);

        // TODO implementation

        return new ResponseEntity<List<StatsValue>>(HttpStatus.OK);
    }

    public ResponseEntity<EventValue> getSubscriptionValue(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Identifier of peerconnection") @RequestParam(value = "peerconnectionId", required = false) String peerconnectionId) {
        log.debug("[getSubscriptionValue] sessionId={} peerconnectionId={}",
                sessionId, peerconnectionId);

        // TODO implementation

        return new ResponseEntity<EventValue>(HttpStatus.OK);
    }

    public ResponseEntity<Void> setUserMedia(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Media URL to take WebRTC user media", required = true) @RequestBody UserMedia body) {
        log.debug("[setUserMedia] sessionId={} userMedia={}", sessionId, body);

        // TODO implementation

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<EventSubscription> subscribeToEvent(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter) and sample rate (in ms)", required = true) @RequestBody Latency body) {
        log.debug("[subscribeToEvent] sessionId={} elementId={} latency={}",
                sessionId, elementId, body);

        // TODO implementation

        return new ResponseEntity<EventSubscription>(HttpStatus.OK);
    }

    public ResponseEntity<EventSubscription> subscribeToLatency(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter), selection of QoE algorithm, and sample rate (in ms)", required = true) @RequestBody Quality body) {
        log.debug("[subscribeToLatency] sessionId={} elementId={} quality={}",
                sessionId, elementId, body);

        // TODO implementation

        return new ResponseEntity<EventSubscription>(HttpStatus.OK);
    }

    public ResponseEntity<EventSubscription> subscribeToQuality(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Event name to be subscribed", required = true) @RequestBody Event body) {
        log.debug("[subscribeToQuality] sessionId={} elementId={} event={}",
                sessionId, elementId, body);

        // TODO implementation

        return new ResponseEntity<EventSubscription>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> webdriverProtocol(
            HttpEntity<String> httpEntity, HttpServletRequest request) {
        final String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        StringBuffer requestUrl = request.getRequestURL();
        String bypassUrl = requestUrl.substring(
                requestUrl.lastIndexOf(contextPath) + contextPath.length());
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        log.debug("[{}] {} {}", methodName, method, bypassUrl);

        // TODO Use Docker instead of local Selenium Server
        String newUrl = "http://localhost:4444/wd/hub" + bypassUrl;
        log.trace("[{}] >> Request : {}", methodName, httpEntity.getBody());

        RestTemplate restTemplate = new RestTemplate();
        String response = "";

        switch (method) {
        case GET:
            response = restTemplate.getForObject(newUrl, String.class);
            break;

        case DELETE:
            restTemplate.delete(newUrl);
            break;

        default:
        case POST:
            response = restTemplate.postForObject(newUrl, httpEntity.getBody(),
                    String.class);
            break;
        }
        log.trace("[{}] << Response: {}", methodName, response);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(response,
                HttpStatus.OK);

        log.debug("[{}] response {}", methodName, responseEntity);

        return responseEntity;
    }

}
