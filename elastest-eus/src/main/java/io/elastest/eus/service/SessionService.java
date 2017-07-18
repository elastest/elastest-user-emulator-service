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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import io.elastest.eus.session.SessionInfo;

/**
 * Session service (WebSocket and session registry).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class SessionService extends TextWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(SessionService.class);

    @Value("${ws.protocol.getSessions}")
    private String wsProtocolGetSessions;

    @Value("${hub.timeout}")
    private String hubTimeout;

    private DockerService dockerService;

    private JsonService jsonService;

    private Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    private Map<String, SessionInfo> sessionRegistry = new ConcurrentHashMap<>();

    public SessionService(DockerService dockerService,
            JsonService jsonService) {
        this.dockerService = dockerService;
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
            for (SessionInfo sessionInfo : sessionRegistry.values()) {
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

    public void removeSession(String sessionId) {
        sessionRegistry.remove(sessionId);
    }

    public void putSession(String sessionId, SessionInfo sessionEntry) {
        sessionRegistry.put(sessionId, sessionEntry);
    }

    public SessionInfo getSession(String sessionId) {
        return sessionRegistry.get(sessionId);
    }

    public Map<String, SessionInfo> getSessionRegistry() {
        return sessionRegistry;
    }

    public void startSessionTimer(SessionInfo sessionInfo) {
        Runnable deleteSession = () -> deleteSession(sessionInfo, true);
        ScheduledExecutorService timeoutExecutor = Executors
                .newScheduledThreadPool(1);
        int timeout = Integer.parseInt(hubTimeout);
        Future<?> timeoutFuture = timeoutExecutor.schedule(deleteSession,
                timeout, TimeUnit.SECONDS);

        sessionInfo.setTimeoutExecutor(timeoutExecutor);
        sessionInfo.setTimeoutFuture(timeoutFuture);
    }

    public void shutdownSessionTimer(SessionInfo sessionInfo) {
        Future<?> timeoutFuture = sessionInfo.getTimeoutFuture();
        if (timeoutFuture != null) {
            timeoutFuture.cancel(true);
        }

        ExecutorService timeoutExecutor = sessionInfo.getTimeoutExecutor();
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    public void deleteSession(SessionInfo sessionInfo, boolean timeout) {
        shutdownSessionTimer(sessionInfo);

        if (timeout) {
            log.info("Deleting session {} due to timeout of {} seconds",
                    sessionInfo.getSessionId(), hubTimeout);
        } else {
            log.info("Deleting session {}", sessionInfo.getSessionId());
        }

        stopAllContainerOfSession(sessionInfo);
        removeSession(sessionInfo.getSessionId());
        if (!sessionInfo.isLiveSession()) {
            sendRemoveSessionToAllClients(sessionInfo);
        }
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
