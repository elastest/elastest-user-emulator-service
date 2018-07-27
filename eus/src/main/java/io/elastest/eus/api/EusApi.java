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

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * API interface.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Api(value = "session")
@RequestMapping("${api.context.path}")
public interface EusApi {

    /**
     * GET /session/{sessionId}/event/{subscriptionId}
     */
    @ApiOperation(value = "Read the value of event for a given subscription", notes = "", response = EventValue.class, tags = {
            "Event subscription", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = EventValue.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = EventValue.class),
            @ApiResponse(code = 404, message = "No such subscription", response = EventValue.class) })
    @RequestMapping(value = "/session/{sessionId}/event/{subscriptionId}", produces = {
            "application/json" }, method = GET)
    ResponseEntity<List<StatsValue>> getStats(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId);

    /**
     * DELETE /session/{sessionId}/event/{subscriptionId}
     */
    @ApiOperation(value = "Remove a subscription", notes = "", response = Void.class, tags = {
            "Event subscription", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = Void.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = Void.class),
            @ApiResponse(code = 404, message = "Subscription not found", response = Void.class) })
    @RequestMapping(value = "/session/{sessionId}/event/{subscriptionId}", method = DELETE)
    ResponseEntity<Void> deleteSubscription(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Subscription identifier (previously subscribed)", required = true) @PathVariable("subscriptionId") String subscriptionId);

    /**
     * GET /session/{sessionId}/element/{elementId}/audio
     */
    @ApiOperation(value = "Read the audio level of a given element (audio|video tag)", notes = "", response = AudioLevel.class, tags = {
            "Basic media evaluation", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = AudioLevel.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = AudioLevel.class),
            @ApiResponse(code = 404, message = "No such element", response = AudioLevel.class) })
    @RequestMapping(value = "/session/{sessionId}/element/{elementId}/audio", produces = {
            "application/json" }, method = GET)
    ResponseEntity<AudioLevel> getAudioLevel(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId);

    /**
     * GET /session/{sessionId}/element/{elementId}/color
     */
    @ApiOperation(value = "Read the RGB color of the coordinates of a given element", notes = "", response = ColorValue.class, tags = {
            "Basic media evaluation", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = ColorValue.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = ColorValue.class),
            @ApiResponse(code = 404, message = "No such element", response = ColorValue.class) })
    @RequestMapping(value = "/session/{sessionId}/element/{elementId}/color", produces = {
            "application/json" }, method = GET)
    ResponseEntity<ColorValue> getColorByCoordinates(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Coordinate in x-axis", defaultValue = "0") @RequestParam(value = "x", required = false, defaultValue = "0") Integer x,
            @ApiParam(value = "Coordinate in y-axis", defaultValue = "0") @RequestParam(value = "y", required = false, defaultValue = "0") Integer y);

    /**
     * GET /session/{sessionId}/stats
     */
    @ApiOperation(value = "Read the WebRTC stats", notes = "", response = StatsValue.class, responseContainer = "List", tags = {
            "WebRTC stats", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = StatsValue.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = StatsValue.class),
            @ApiResponse(code = 404, message = "No such subscription", response = StatsValue.class) })
    @RequestMapping(value = "/session/{sessionId}/stats", produces = {
            "application/json" }, method = GET)
    ResponseEntity<EventValue> getSubscriptionValue(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Identifier of peerconnection") @RequestParam(value = "peerconnectionId", required = false) String peerconnectionId);

    /**
     * POST /session/{sessionId}/usermedia
     */
    @ApiOperation(value = "Set user media for WebRTC", notes = "", response = Void.class, tags = {
            "WebRTC user media", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = Void.class),
            @ApiResponse(code = 400, message = "Invalid media", response = Void.class),
            @ApiResponse(code = 404, message = "URL not found", response = Void.class) })
    @RequestMapping(value = "/session/{sessionId}/usermedia", consumes = {
            "application/json" }, method = POST)
    ResponseEntity<Void> setUserMedia(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Media URL to take WebRTC user media", required = true) @RequestBody UserMedia body);

    /**
     * POST /session/{sessionId}/element/{elementId}/latency
     */
    @ApiOperation(value = "Measure end-to-end latency of a WebRTC session", notes = "The E2E latency calculation is done comparing the media in P2P WebRTC communication (presenter-viewer)", response = EventSubscription.class, tags = {
            "Advance media evaluation", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = EventSubscription.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = EventSubscription.class),
            @ApiResponse(code = 404, message = "No such element", response = EventSubscription.class) })
    @RequestMapping(value = "/session/{sessionId}/element/{elementId}/latency", produces = {
            "application/json" }, consumes = {
                    "application/json" }, method = POST)
    ResponseEntity<EventSubscription> subscribeToEvent(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter) and sample rate (in ms)", required = true) @RequestBody Latency body);

    /**
     * POST /session/{sessionId}/element/{elementId}/quality
     */
    @ApiOperation(value = "Measure quality (audio|video) of a WebRTC session", notes = "The quality indicator is calculated comparing the media in P2P WebRTC communication (presenter-viewer)", response = EventSubscription.class, tags = {
            "Advance media evaluation", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = EventSubscription.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = EventSubscription.class),
            @ApiResponse(code = 404, message = "No such element", response = EventSubscription.class) })
    @RequestMapping(value = "/session/{sessionId}/element/{elementId}/quality", produces = {
            "application/json" }, consumes = {
                    "application/json" }, method = POST)
    ResponseEntity<EventSubscription> subscribeToLatency(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Definition of WebRTC producer (presenter), selection of QoE algorithm, and sample rate (in ms)", required = true) @RequestBody Quality body);

    /**
     * POST /session/{sessionId}/element/{elementId}/event
     */
    @ApiOperation(value = "Subscribe to a given event within an element", notes = "", response = EventSubscription.class, tags = {
            "Event subscription", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = EventSubscription.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = EventSubscription.class),
            @ApiResponse(code = 403, message = "Forbidden", response = EventSubscription.class),
            @ApiResponse(code = 404, message = "No such element", response = EventSubscription.class) })
    @RequestMapping(value = "/session/{sessionId}/element/{elementId}/event", produces = {
            "application/json" }, consumes = {
                    "application/json" }, method = POST)
    ResponseEntity<EventSubscription> subscribeToQuality(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "Element identifier (previously located)", required = true) @PathVariable("elementId") String elementId,
            @ApiParam(value = "Event name to be subscribed", required = true) @RequestBody Event body);

    /* *************************** */
    /* ********* Session ********* */
    /* *************************** */

    /**
     * GET/POST/DELETE /session/**
     *
     * W3C WebDriver operations for sessions
     */
    @ApiOperation(value = "W3C WebDriver standard sessions operations", notes = "", response = String.class, tags = {
            "W3C WebDriver" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 404, message = "No such element"),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/session/**", produces = {
            "application/json" }, method = { GET, POST, DELETE })
    ResponseEntity<String> session(HttpEntity<String> httpEntity,
            HttpServletRequest request);

    /**
     * GET/POST/DELETE /execution/{key}/session/**
     *
     * W3C WebDriver operations for sessions
     */
    @ApiOperation(value = "Start session by given Execution", notes = "", response = String.class, tags = {
            "W3C WebDriver" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 404, message = "No such element"),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/{key}/session/**", produces = {
            "application/json" }, method = { GET, POST, DELETE })
    ResponseEntity<String> sessionFromExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            HttpEntity<String> httpEntity, HttpServletRequest request);

    /* ************************************************* */
    /* ********* Register/Unregister execution ********* */
    /* ************************************************* */

    /**
     * POST /execution/register
     *
     * Start MP4 recording
     */
    @ApiOperation(value = "Register execution", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid execution data or not registered", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/register", produces = {
            "application/json" }, consumes = {
                    "application/json" }, method = { POST })
    ResponseEntity<String> registerExecution(
            @ApiParam(value = "The Execution Data", required = true) @Valid @RequestBody ExecutionData executionData);

    /**
     * POST /execution/unregister
     *
     * Start MP4 recording
     */
    @ApiOperation(value = "Unregister execution", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid execution data or not registered", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/unregister/{key}", produces = {
            "application/json" }, method = { DELETE })
    ResponseEntity<String> unregisterExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key);

    /* ************************** */
    /* ********* Status ********* */
    /* ************************** */

    /**
     * GET /status
     *
     * W3C WebDriver operations for status
     */
    @ApiOperation(value = "W3C WebDriver standard get status operation", notes = "", response = String.class, tags = {
            "W3C WebDriver" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/status", produces = {
            "application/json" }, method = { GET })
    ResponseEntity<String> getStatus();

    /**
     * GET /execution/{key}/status
     *
     * W3C WebDriver operations for status
     */
    @ApiOperation(value = "W3C WebDriver standard get status operation", notes = "", response = String.class, tags = {
            "W3C WebDriver" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/{key}/status", produces = {
            "application/json" }, method = { GET })
    ResponseEntity<String> getStatusExecution(
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key);

    /* ************************* */
    /* ********** Vnc ********** */
    /* ************************* */

    /**
     * GET /session/{sessionId}/vnc
     *
     * Get VNC session
     */
    @ApiOperation(value = "Get VNC session", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/session/{sessionId}/vnc", produces = {
            "text/plain" }, method = { GET })
    ResponseEntity<String> vnc(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId);

    /**
     * GET /execution/{key}/session/{sessionId}/vnc
     *
     * Get VNC session
     */
    @ApiOperation(value = "Get VNC session", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/{key}/session/{sessionId}/vnc", produces = {
            "text/plain" }, method = { GET })
    ResponseEntity<String> executionVnc(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key);

    /* *************************** */
    /* ******** Recording ******** */
    /* *************************** */
    /**
     * GET/DELETE /session/{sessionId}/recording
     *
     * Get MP4 recording
     */
    @ApiOperation(value = "Handle recordings", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier or hub container name", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/session/{sessionId}/recording", produces = {
            "text/plain" }, method = { GET, DELETE })
    ResponseEntity<String> recording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            HttpServletRequest request);

    /**
     * GET/DELETE /execution/{key}/session/{sessionId}/recording
     *
     * Get MP4 recording
     */
    @ApiOperation(value = "Handle recordings", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier or hub container name", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/{key}/session/{sessionId}/recording", produces = {
            "text/plain" }, method = { GET, DELETE })
    ResponseEntity<String> executionRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            HttpServletRequest request,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key);

    /* ***************************** */
    /* ****** Start Recording ****** */
    /* ***************************** */

    /**
     * POST /session/{sessionId}/recording/{hubContainerName}/start
     *
     * Start MP4 recording
     */
    @ApiOperation(value = "Start recording", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier or hub container name", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/session/{sessionId}/recording/{hubContainerName}/start", produces = {
            "text/plain" }, consumes = { "text/plain" }, method = { POST })
    ResponseEntity<String> startRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName,
            @ApiParam(value = "The Video Name", required = true) @Valid @RequestBody String videoName);

    /**
     * POST
     * /execution/{key}/session/{sessionId}/recording/{hubContainerName}/start
     *
     * Start MP4 recording
     */
    @ApiOperation(value = "Start recording", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier or hub container name", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/{key}/session/{sessionId}/recording/{hubContainerName}/start", produces = {
            "text/plain" }, consumes = { "text/plain" }, method = { POST })
    ResponseEntity<String> startExecutionRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName,
            @ApiParam(value = "The Video Name", required = true) @Valid @RequestBody String videoName,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key);

    /* *************************** */
    /* ****** Stop Recording****** */
    /* *************************** */

    /**
     * DELETE /session/{sessionId}/recording/{hubContainerName}/stop
     *
     * Stop MP4 recording
     */
    @ApiOperation(value = "Stop recording", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier or hub container name", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/session/{sessionId}/recording/{hubContainerName}/stop", produces = {
            "text/plain" }, method = { DELETE })
    ResponseEntity<String> stopRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName);

    /**
     * DELETE
     * /execution/{key}/session/{sessionId}/recording/{hubContainerName}/stop
     *
     * Stop MP4 recording
     */
    @ApiOperation(value = "Stop recording", notes = "", response = String.class, tags = {
            "Remote control" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation", response = String.class),
            @ApiResponse(code = 400, message = "Invalid session identifier or hub container name", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error", response = String.class) })
    @RequestMapping(value = "/execution/{key}/session/{sessionId}/recording/{hubContainerName}/stop", produces = {
            "text/plain" }, method = { DELETE })
    ResponseEntity<String> stopExecutionRecording(
            @ApiParam(value = "Session identifier (previously established)", required = true) @PathVariable("sessionId") String sessionId,
            @ApiParam(value = "The Hub Container Name", required = true) @PathVariable("hubContainerName") String hubContainerName,
            @ApiParam(value = "The Key of the execution)", required = true) @PathVariable("key") String key,
            HttpServletRequest request);

}
