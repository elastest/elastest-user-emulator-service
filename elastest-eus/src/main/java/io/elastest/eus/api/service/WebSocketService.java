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
package io.elastest.eus.api.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import io.elastest.eus.api.session.SessionInfo;

/**
 * WebSocket service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class WebSocketService extends TextWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    @Value("${ws.protocol.getSessions}")
    private String wsProtocolGetSessions;

    private RegistryService registryService;

    private JsonService jsonService;

    private Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public WebSocketService(RegistryService registryService,
            JsonService jsonService) {
        this.registryService = registryService;
        this.jsonService = jsonService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session,
            TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();
        log.debug("Incomming message {} from session {}", payload, sessionId);

        if (payload.equalsIgnoreCase(wsProtocolGetSessions)) {
            log.trace("{} received", payload);
            sendAllSessionsInfoToAllClients();
        } else {
            log.warn("Non recognized message {}", payload);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        super.afterConnectionEstablished(session);
        String sessionId = session.getId();
        log.debug("WebSocket connection {} established ", sessionId);

        activeSessions.put(sessionId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
            CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        String sessionId = session.getId();
        log.debug("WebSocket connection {} closed ", sessionId);

        activeSessions.remove(sessionId);
    }

    public void sendTextMessage(WebSocketSession session, String message) {
        TextMessage textMessage = new TextMessage(message);
        try {
            log.trace("Sending {} to session {}", message, session.getId());
            session.sendMessage(textMessage);
        } catch (IOException e) {
            log.warn("Error sending message {} in session {}", message,
                    session.getId(), e);
        }
    }

    public void sendAllSessionsInfoToAllClients() {
        for (WebSocketSession session : activeSessions.values()) {
            for (SessionInfo sessionInfo : registryService.getSessionRegistry()
                    .values()) {
                sendTextMessage(session,
                        jsonService.newSessionJson(sessionInfo).toString());
            }
        }
    }

    public void sendNewSessionToAllClients(SessionInfo sessionInfo) {
        for (WebSocketSession session : activeSessions.values()) {
            sendTextMessage(session,
                    jsonService.newSessionJson(sessionInfo).toString());
        }
    }

    public void sendRemoveSessionToAllClients(SessionInfo sessionInfo) {
        for (WebSocketSession session : activeSessions.values()) {
            sendTextMessage(session,
                    jsonService.removeSessionJson(sessionInfo).toString());
        }
    }

    public boolean isActiveSessions() {
        return !activeSessions.isEmpty();
    }

}
