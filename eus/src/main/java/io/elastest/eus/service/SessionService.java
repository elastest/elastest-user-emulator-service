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

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.json.WebSocketNewLiveSession;
import io.elastest.eus.json.WebSocketNewSession;
import io.elastest.eus.json.WebSocketRecordedSession;
import io.elastest.eus.json.WebSocketRemoveSession;
import io.elastest.eus.platform.manager.PlatformManager;
import io.elastest.eus.session.SessionManager;

/**
 * Session service (WebSocket and session registry).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */

public class SessionService extends TextWebSocketHandler implements Observer {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${ws.protocol.getSessions}")
    private String wsProtocolGetSessions;

    @Value("${ws.protocol.getLiveSessions}")
    private String wsProtocolGetLiveSessions;

    @Value("${ws.protocol.getRecordings}")
    private String wsProtocolGetRecordings;

    private Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private Map<String, SessionManager> sessionRegistry = new ConcurrentHashMap<>();

    private EusJsonService jsonService;
    private RecordingService recordingService;

    public SessionService(EusJsonService jsonService,
            RecordingService recordingService) {
        this.jsonService = jsonService;
        this.recordingService = recordingService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException {
        String sessionId = session.getId();
        String payload = message.getPayload();
        log.debug("Incoming message {} from session {}", payload, sessionId);

        if (payload.equalsIgnoreCase(wsProtocolGetSessions)) {
            log.trace("{} received", payload);
            sendAllSessionsInfoToAllClients();
        } else if (payload.equalsIgnoreCase(wsProtocolGetLiveSessions)) {
            log.trace("{} received", payload);
            sendAllLiveSessionsInfoToAllClients();
        } else if (payload.equalsIgnoreCase(wsProtocolGetRecordings)) {
            log.trace("{} received", payload);
            sendAllRecordingsToAllClients();
        } else {
            log.warn("Non recognized message {}", payload);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        super.afterConnectionEstablished(session);
        String sessionId = session.getId();
        log.debug("WebSocket connection {} established", sessionId);

        activeSessions.put(sessionId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
            CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        String sessionId = session.getId();
        log.debug("WebSocket connection {} closed", sessionId);

        activeSessions.remove(sessionId);
    }

    public void sendTextMessage(WebSocketSession session, String message)
            throws IOException {
        TextMessage textMessage = new TextMessage(message);
        log.trace("Sending {} to session {}", message, session.getId());
        session.sendMessage(textMessage);
    }

    /* ****************** */
    /* *** Recordings *** */
    /* ****************** */

    // All recordings from default path (not from executions)
    public void sendAllRecordingsToAllClients() throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            for (String fileContent : recordingService
                    .getStoredMetadataContent()) {
                sendTextMessage(session, fileContent);
            }
        }
    }

    public void sendRecordingToAllClients(SessionManager sessionManager)
            throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            WebSocketRecordedSession recordedSession = new WebSocketRecordedSession(
                    sessionManager);
            log.debug("Sending recording {} to session {}", recordedSession,
                    session);
            sendTextMessage(session, jsonService.objectToJson(recordedSession));
        }
    }

    /* *********************************************** */
    /* *** Generic New Session (live and non live) *** */
    /* *********************************************** */

    public void sendNewSessionToAllClients(SessionManager sessionManager)
            throws IOException {
        if (activeWebSocketSessions()) {
            this.sendNewSessionToAllClients(sessionManager, true);
        }
    }

    public void sendNewSessionToAllClients(SessionManager sessionManager,
            boolean printDebug) throws IOException {
        if (activeWebSocketSessions()) {
            if (!sessionManager.isLiveSession()) {
                sendNewNormalSessionToAllClients(sessionManager, printDebug);
            } else {
                sendNewLiveSessionToAllClients(sessionManager, printDebug);
            }
        }
    }

    /* ************************ */
    /* *** Non-Live Session *** */
    /* ************************ */

    public void sendNewNormalSessionToAllClients(SessionManager sessionManager)
            throws IOException {
        this.sendNewNormalSessionToAllClients(sessionManager, true);
    }

    public void sendNewNormalSessionToAllClients(SessionManager sessionManager,
            boolean printDebug) throws IOException {
        if (!sessionManager.isLiveSession()) {
            for (WebSocketSession session : activeSessions.values()) {
                sendNewNormalSessionToGivenSessionClient(session,
                        sessionManager, printDebug);
            }
        }
    }

    public void sendNewNormalSessionToGivenSessionClient(
            WebSocketSession session, SessionManager sessionManager,
            boolean printDebug) throws JsonProcessingException, IOException {
        WebSocketNewSession newSession = new WebSocketNewSession(
                sessionManager);
        if (printDebug) {
            log.debug("Sending newSession message {} to session {}", newSession,
                    session);
        }
        sendTextMessage(session, jsonService.objectToJson(newSession));
    }

    public void sendAllSessionsInfoToAllClients() throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            for (SessionManager sessionManager : sessionRegistry.values()) {
                if (!sessionManager.isLiveSession()) {
                    sendNewNormalSessionToGivenSessionClient(session,
                            sessionManager, true);
                }
            }
        }
    }

    /* ******************** */
    /* *** Live Session *** */
    /* ******************** */

    public void sendNewLiveSessionToAllClients(SessionManager sessionManager)
            throws IOException {
        this.sendNewLiveSessionToAllClients(sessionManager, true);
    }

    public void sendNewLiveSessionToAllClients(SessionManager sessionManager,
            boolean printDebug) throws IOException {
        if (sessionManager.isLiveSession()) {
            for (WebSocketSession session : activeSessions.values()) {
                this.sendNewLiveSessionToGivenSessionClient(session,
                        sessionManager, printDebug);
            }
        }
    }

    public void sendNewLiveSessionToGivenSessionClient(WebSocketSession session,
            SessionManager sessionManager, boolean printDebug)
            throws JsonProcessingException, IOException {
        WebSocketNewLiveSession newLiveSession = new WebSocketNewLiveSession(
                sessionManager);
        if (printDebug) {
            log.debug("Sending newLiveSession message {} to session {}",
                    newLiveSession, session);
        }
        sendTextMessage(session, jsonService.objectToJson(newLiveSession));
    }

    public void sendAllLiveSessionsInfoToAllClients() throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            for (SessionManager sessionManager : sessionRegistry.values()) {
                if (sessionManager.isLiveSession()) {
                    sendNewLiveSessionToGivenSessionClient(session,
                            sessionManager, true);
                }
            }
        }
    }

    /* ********************** */
    /* *** Remove Session *** */
    /* ********************** */

    public void sendRemoveSessionToAllClients(SessionManager sessionManager)
            throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            WebSocketRemoveSession removeSession = new WebSocketRemoveSession(
                    sessionManager);
            log.debug("Sending remove session message {} to session {}",
                    removeSession, session);
            sendTextMessage(session, jsonService.objectToJson(removeSession));
        }
    }

    /* ************** */
    /* *** Others *** */
    /* ************** */

    public void removeSession(String sessionId) {
        if (sessionId != null) {
            log.debug("Remove session {}", sessionId);
            sessionRegistry.remove(sessionId);
        }
    }

    public boolean activeWebSocketSessions() {
        return !activeSessions.isEmpty();
    }

    public void putSession(String sessionId, SessionManager sessionEntry) {
        sessionRegistry.put(sessionId, sessionEntry);
    }

    public Optional<SessionManager> getSession(String sessionId) {
        if (sessionRegistry.containsKey(sessionId)) {
            return Optional.of(sessionRegistry.get(sessionId));
        } else {
            return Optional.empty();
        }
    }

    public void updateSessionManager(String sessionId,
            SessionManager sessionManager) {
        sessionRegistry.put(sessionId, sessionManager);
    }

    public Map<String, SessionManager> getSessionRegistry() {
        return sessionRegistry;
    }

    public void stopAllContainerOfSession(SessionManager sessionManager)
            throws Exception {
        String hubContainerName = sessionManager.getHubContainerName();
        int killTimeoutInSeconds = 10;
        PlatformManager platformManager = sessionManager.getPlatformManager();
        if (hubContainerName != null
                && platformManager.existServiceWithName(hubContainerName)) {
            platformManager.removeServiceWithTimeout(hubContainerName,
                    killTimeoutInSeconds);
        }

        String vncContainerName = sessionManager.getVncContainerName();
        if (vncContainerName != null
                && platformManager.existServiceWithName(vncContainerName)) {
            platformManager.removeServiceWithTimeout(vncContainerName,
                    killTimeoutInSeconds);
        }

        String awsInstance = sessionManager.getAwsInstanceId();

        if (sessionManager.isAWSSession() && awsInstance != null
                && platformManager.existServiceWithName(awsInstance)) {
            platformManager.removeServiceWithTimeout(awsInstance,
                    killTimeoutInSeconds);
        }

    }

    boolean isLive(String jsonMessage) {
        boolean out = false;
        try {
            out = jsonService
                    .jsonToObject(jsonMessage, WebDriverCapabilities.class)
                    .getDesiredCapabilities().isLive();
        } catch (Exception e) {
            log.warn(
                    "Exception {} checking if session is live. JSON message: {}",
                    e.getMessage(), jsonMessage);
        }
        log.trace("Live session = {} -- JSON message: {}", out, jsonMessage);
        return out;
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            sendNewSessionToAllClients((SessionManager) arg, false);
        } catch (IOException io) {
            log.error("Error sending browser status to all clients: {}",
                    io.getMessage());
        }
    }

}
