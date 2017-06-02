package io.elastest.eus.api;

import io.elastest.eus.api.model.AudioLevel;
import io.elastest.eus.api.model.ColorValue;
import io.elastest.eus.api.model.EventValue;
import io.elastest.eus.api.model.StatsValue;
import io.elastest.eus.api.model.UserMedia;
import io.elastest.eus.api.model.EventSubscription;
import io.elastest.eus.api.model.Latency;
import io.elastest.eus.api.model.Quality;
import io.elastest.eus.api.model.Event;

import io.swagger.annotations.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2017-06-01T16:29:27.571+02:00")

@Controller
public class SessionApiController implements SessionApi {

    public ResponseEntity<Void> deleteSubscription(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        
@ApiParam(value = "Subscription identifier (previously subscribed)",required=true ) @PathVariable("subscriptionId") String subscriptionId


) {
        // do some magic!
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<AudioLevel> getAudioLevelByElement(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        
@ApiParam(value = "Element identifier (previously located)",required=true ) @PathVariable("elementId") String elementId


) {
        // do some magic!
        return new ResponseEntity<AudioLevel>(HttpStatus.OK);
    }

    public ResponseEntity<ColorValue> getRGBColorByCoordinates(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        
@ApiParam(value = "Element identifier (previously located)",required=true ) @PathVariable("elementId") String elementId


,
        @ApiParam(value = "Coordinate in x-axis", defaultValue = "0") @RequestParam(value = "x", required = false, defaultValue="0") Integer x



,
        @ApiParam(value = "Coordinate in y-axis", defaultValue = "0") @RequestParam(value = "y", required = false, defaultValue="0") Integer y



) {
        // do some magic!
        return new ResponseEntity<ColorValue>(HttpStatus.OK);
    }

    public ResponseEntity<EventValue> getSubscriptionValue(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        
@ApiParam(value = "Subscription identifier (previously subscribed)",required=true ) @PathVariable("subscriptionId") String subscriptionId


) {
        // do some magic!
        return new ResponseEntity<EventValue>(HttpStatus.OK);
    }

    public ResponseEntity<List<StatsValue>> getWebRTCStats(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        @ApiParam(value = "Identifier of peerconnection") @RequestParam(value = "peerconnectionId", required = false) String peerconnectionId



) {
        // do some magic!
        return new ResponseEntity<List<StatsValue>>(HttpStatus.OK);
    }

    public ResponseEntity<Void> seetUserMediaForWebRTC(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        

@ApiParam(value = "Media URL to take WebRTC user media" ,required=true ) @RequestBody UserMedia body

) {
        // do some magic!
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<EventSubscription> setE2ELatencyOfWebRTCSession(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        
@ApiParam(value = "Element identifier (previously located)",required=true ) @PathVariable("elementId") String elementId


,
        

@ApiParam(value = "Definition of WebRTC producer (presenter) and sample rate (in ms)" ,required=true ) @RequestBody Latency body

) {
        // do some magic!
        return new ResponseEntity<EventSubscription>(HttpStatus.OK);
    }

    public ResponseEntity<EventSubscription> setMeasureQAOfWebRTCSession(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        
@ApiParam(value = "Element identifier (previously located)",required=true ) @PathVariable("elementId") String elementId


,
        

@ApiParam(value = "Definition of WebRTC producer (presenter), selection of QoE algorithm, and sample rate (in ms)" ,required=true ) @RequestBody Quality body

) {
        // do some magic!
        return new ResponseEntity<EventSubscription>(HttpStatus.OK);
    }

    public ResponseEntity<EventSubscription> subscribeToEvent(
@ApiParam(value = "Session identifier (previously established)",required=true ) @PathVariable("sessionId") String sessionId


,
        
@ApiParam(value = "Element identifier (previously located)",required=true ) @PathVariable("elementId") String elementId


,
        

@ApiParam(value = "Event name to be subscribed" ,required=true ) @RequestBody Event body

) {
        // do some magic!
        return new ResponseEntity<EventSubscription>(HttpStatus.OK);
    }

}