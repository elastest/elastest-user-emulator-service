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

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for JSON utilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class JsonService {

    private final Logger log = LoggerFactory.getLogger(JsonService.class);

    @Value("${webdriver.capabilities}")
    private String webdriverCapabilities;

    @Value("${webdriver.browserName}")
    private String webdriverBrowserName;

    @Value("${webdriver.version}")
    private String webdriverVersion;

    @Value("${webdriver.platform}")
    private String webdriverPlatform;

    @Value("${webdriver.live}")
    private String webdriverLive;

    @Value("${webdriver.sessionId}")
    private String webdriverSessionId;

    @Value("${webdriver.session.message}")
    private String webdriverSessionMessage;

    @Value("${ws.protocol.newSession}")
    private String wsProtocolNewSession;

    @Value("${ws.protocol.recordedSession}")
    private String wsProtocolRecordedSession;

    @Value("${ws.protocol.removeSession}")
    private String wsProtocolRemoveSession;

    @Value("${ws.protocol.sessionId}")
    private String wsProtocolSessionId;

    @Value("${ws.protocol.id}")
    private String wsProtocolId;

    @Value("${ws.protocol.url}")
    private String wsProtocolUrl;

    @Value("${ws.protocol.path}")
    private String wsProtocolPath;

    @Value("${ws.protocol.browser}")
    private String wsProtocolBrowser;

    @Value("${ws.protocol.version}")
    private String wsProtocolVersion;

    @Value("${ws.protocol.creationTime}")
    private String wsProtocolCreationTime;

    @Value("${ws.protocol.ready}")
    private String wsProtocolReady;

    @Value("${ws.protocol.message}")
    private String wsProtocolMessage;

    private JSONObject getCapabilities(String jsonMessage) {
        return (JSONObject) string2Json(jsonMessage).get(webdriverCapabilities);
    }

    public String getBrowser(String jsonMessage) {
        return (String) getCapabilities(jsonMessage).get(webdriverBrowserName);
    }

    public String getSessionIdFromResponse(String jsonMessage) {
        return (String) ((JSONObject) string2Json(jsonMessage))
                .get(webdriverSessionId);
    }

    public String getVersion(String jsonMessage) {
        return (String) getCapabilities(jsonMessage).get(webdriverVersion);
    }

    public String getPlatform(String jsonMessage) {
        return (String) getCapabilities(jsonMessage).get(webdriverPlatform);
    }

    private JSONObject string2Json(String jsonMessage) {
        return new JSONObject(jsonMessage);
    }

    public Optional<String> getSessionIdFromPath(String path) {
        Optional<String> out = Optional.empty();
        int i = path.indexOf(webdriverSessionMessage);

        if (i != -1) {
            int j = path.indexOf('/', i + webdriverSessionMessage.length());
            if (j != -1) {
                int k = path.indexOf('/', j + 1);
                int cut = (k == -1) ? path.length() : k;

                String sessionId = path.substring(j + 1, cut);
                out = Optional.of(sessionId);
            }
        }

        log.trace("getSessionIdFromPath -- path: {} sessionId {}", path, out);

        return out;
    }

    public boolean isPostSessionRequest(HttpMethod method, String context) {
        return method == POST && context.equals(webdriverSessionMessage);
    }

    public boolean isDeleteSessionRequest(HttpMethod method, String context) {
        return method == DELETE && context.startsWith(webdriverSessionMessage)
                && countCharsInString(context, '/') == 2;
    }

    public int countCharsInString(String string, char c) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public boolean isLive(String jsonMessage) {
        boolean out = false;
        try {
            out = (Boolean) getCapabilities(jsonMessage).get(webdriverLive);
            log.trace("Received message from a live session");
        } catch (Exception e) {
            log.trace("Received message from a regular session (non-live)");
        }
        return out;
    }

    public JSONObject newSessionJson(SessionInfo sessionInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(wsProtocolNewSession, sessionInfoToJson(sessionInfo));
        return jsonObject;
    }

    public JSONObject recordedSessionJson(SessionInfo sessionInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(wsProtocolRecordedSession, registryJson(sessionInfo));
        return jsonObject;
    }

    public JSONObject removeSessionJson(SessionInfo sessionInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(wsProtocolRemoveSession, sessionInfoToJson(sessionInfo));
        return jsonObject;
    }

    public JSONObject sessionInfoToJson(SessionInfo sessionInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(wsProtocolId, sessionInfo.getSessionId());
        jsonObject.put(wsProtocolUrl, sessionInfo.getVncUrl());
        jsonObject.put(wsProtocolBrowser, sessionInfo.getBrowser());
        jsonObject.put(wsProtocolVersion, sessionInfo.getVersion());
        jsonObject.put(wsProtocolCreationTime, sessionInfo.getCreationTime());
        return jsonObject;
    }

    public JSONObject registryJson(SessionInfo sessionInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(wsProtocolId, sessionInfo.getSessionId());
        jsonObject.put(wsProtocolPath, sessionInfo.getRecordingPath());
        jsonObject.put(wsProtocolBrowser, sessionInfo.getBrowser());
        jsonObject.put(wsProtocolVersion, sessionInfo.getVersion());
        jsonObject.put(wsProtocolCreationTime, sessionInfo.getCreationTime());
        return jsonObject;
    }

    public JSONObject getStatus() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(wsProtocolReady, true);
        jsonObject.put(wsProtocolMessage, "EUS ready");
        return jsonObject;
    }

}
