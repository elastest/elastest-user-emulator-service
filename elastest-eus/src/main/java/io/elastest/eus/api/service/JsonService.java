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

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

/**
 * Service implementation for JSON utilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class JsonService {

    private final Logger log = LoggerFactory.getLogger(JsonService.class);

    public static final String CAPABILITES = "desiredCapabilities";
    public static final String BROWSERNAME = "browserName";
    public static final String VERSION = "version";
    public static final String PLATFORM = "platform";
    public static final String SESSION_ID = "sessionId";

    private static final String SESSION_MESSAGE = "/session";

    private JSONObject getCapabilities(String jsonMessage) {
        return (JSONObject) string2Json(jsonMessage).get(CAPABILITES);
    }

    public String getBrowser(String jsonMessage) {
        return (String) getCapabilities(jsonMessage).get(BROWSERNAME);
    }

    public String getSessionIdFromResponse(String jsonMessage) {
        return (String) ((JSONObject) string2Json(jsonMessage)).get(SESSION_ID);
    }

    public String getVersion(String jsonMessage) {
        return (String) getCapabilities(jsonMessage).get(VERSION);
    }

    public String getPlatform(String jsonMessage) {
        return (String) getCapabilities(jsonMessage).get(PLATFORM);
    }

    private JSONObject string2Json(String jsonMessage) {
        return new JSONObject(jsonMessage);
    }

    public Optional<String> getSessionIdFromPath(String path) {
        Optional<String> out = Optional.empty();
        int i = path.indexOf(SESSION_MESSAGE);

        if (i != -1) {
            int j = path.indexOf('/', i + SESSION_MESSAGE.length());
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

    public static String getSessionMessage() {
        return SESSION_MESSAGE;
    }

    public boolean isPostSessionRequest(HttpMethod method, String context) {
        return method == POST && context.equals(SESSION_MESSAGE);
    }

    public boolean isDeleteSessionRequest(HttpMethod method, String context) {
        return method == DELETE && context.startsWith(SESSION_MESSAGE)
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

}
