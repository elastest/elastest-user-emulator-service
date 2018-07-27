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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import io.elastest.eus.json.WebSocketNewSession;
import io.elastest.eus.json.WebSocketRecordedSession;
import io.elastest.eus.json.WebSocketRemoveSession;
import io.elastest.eus.session.SessionInfo;

/**
 * Session service (WebSocket and session registry).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class SessionService extends TextWebSocketHandler {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${ws.protocol.getSessions}")
    private String wsProtocolGetSessions;

    @Value("${ws.protocol.getRecordings}")
    private String wsProtocolGetRecordings;

    @Value("${hub.timeout}")
    private String hubTimeout;

    @Value("${registry.folder}")
    private String registryFolder;

    private Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private Map<String, SessionInfo> sessionRegistry = new ConcurrentHashMap<>();

    private EusDockerService dockerService;
    private EusJsonService jsonService;
    private RecordingService recordingService;

    public SessionService(EusDockerService dockerService, EusJsonService jsonService,
            RecordingService recordingService) {
        this.dockerService = dockerService;
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

    public void sendAllSessionsInfoToAllClients() throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            for (SessionInfo sessionInfo : sessionRegistry.values()) {
                WebSocketNewSession newSession = new WebSocketNewSession(
                        sessionInfo);
                log.debug("Sending newSession message {} to session {} [all]",
                        newSession, session);
                sendTextMessage(session, jsonService.objectToJson(newSession));
            }
        }
    }

    // All recordings from default path (not from executions)
    public void sendAllRecordingsToAllClients() throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            for (String fileContent : recordingService
                    .getStoredMetadataContent()) {
                sendTextMessage(session, fileContent);
            }
        }
    }

    public void sendRecordingToAllClients(SessionInfo sessionInfo)
            throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            WebSocketRecordedSession recordedSession = new WebSocketRecordedSession(
                    sessionInfo);
            log.debug("Sending recording {} to session {}", recordedSession,
                    session);
            sendTextMessage(session, jsonService.objectToJson(recordedSession));
        }
    }

    public void sendNewSessionToAllClients(SessionInfo sessionInfo)
            throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            WebSocketNewSession newSession = new WebSocketNewSession(
                    sessionInfo);
            log.debug("Sending newSession message {} to session {}", newSession,
                    session);
            sendTextMessage(session, jsonService.objectToJson(newSession));
        }
    }

    public boolean activeWebSocketSessions() {
        return !activeSessions.isEmpty();
    }

    public void sendRemoveSessionToAllClients(SessionInfo sessionInfo)
            throws IOException {
        for (WebSocketSession session : activeSessions.values()) {
            WebSocketRemoveSession removeSession = new WebSocketRemoveSession(
                    sessionInfo);
            log.debug("Sending remove session message {} to session {}",
                    removeSession, session);
            sendTextMessage(session, jsonService.objectToJson(removeSession));
        }
    }

    public void removeSession(String sessionId) {
        if (sessionId != null) {
            log.debug("Remove session {}", sessionId);
            sessionRegistry.remove(sessionId);
        }
    }

    public void putSession(String sessionId, SessionInfo sessionEntry) {
        sessionRegistry.put(sessionId, sessionEntry);
    }

    public Optional<SessionInfo> getSession(String sessionId) {
        if (sessionRegistry.containsKey(sessionId)) {
            return Optional.of(sessionRegistry.get(sessionId));
        } else {
            return Optional.empty();
        }
    }

    public Map<String, SessionInfo> getSessionRegistry() {
        return sessionRegistry;
    }

    public void stopAllContainerOfSession(SessionInfo sessionInfo) {
        String hubContainerName = sessionInfo.getHubContainerName();
        if (hubContainerName != null) {
            dockerService.stopAndRemoveContainer(hubContainerName);
        }

        String vncContainerName = sessionInfo.getVncContainerName();
        if (vncContainerName != null) {
            dockerService.stopAndRemoveContainer(vncContainerName);
        }
    }

}
