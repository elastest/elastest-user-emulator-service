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
package io.elastest.eus.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import io.elastest.eus.api.service.SessionInfo;

/**
 * EUS WebSocket handler.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class EusWebSocketHandler extends TextWebSocketHandler {

    private final Logger log = LoggerFactory
            .getLogger(EusWebSocketHandler.class);

    private Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    private static final String GET_SESSIONS_MESSAGE = "getSessions";

    @Override
    public void handleTextMessage(WebSocketSession session,
            TextMessage message) {
        String sessionId = session.getId();
        String payload = message.getPayload();
        log.debug("Incomming message {} from session {}", payload, sessionId);

        switch (payload) {
        case GET_SESSIONS_MESSAGE:
            log.debug("{} received", payload);
            // TODO send proper response
            break;

        default:
            // TODO: so far, it responses with the same message
            sendTextMessage(session, payload);
            break;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        super.afterConnectionEstablished(session);
        String sessionId = session.getId();
        log.trace("WebSocket connection {} established ", sessionId);

        activeSessions.put(sessionId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
            CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        String sessionId = session.getId();
        log.trace("WebSocket connection {} closed ", sessionId);

        activeSessions.remove(sessionId);
    }

    public void sendTextMessage(WebSocketSession session, String message) {
        TextMessage textMessage = new TextMessage(message);
        try {
            session.sendMessage(textMessage);
        } catch (IOException e) {
            log.warn("Error sending message {} in session {}", message,
                    session.getId(), e);
        }
    }

    public void sendSessionInfoToAllClients(SessionInfo sessionInfo) {
        for (WebSocketSession session : activeSessions.values()) {
            sendTextMessage(session, sessionInfo.toJson());
        }
    }

    public boolean isActiveSessions() {
        return !activeSessions.isEmpty();
    }

}
