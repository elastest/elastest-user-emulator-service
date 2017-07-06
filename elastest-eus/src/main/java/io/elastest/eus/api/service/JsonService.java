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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${webdriver.capabilities}")
    private String webdriverCapabilities;

    @Value("${webdriver.browserName}")
    private String webdriverBrowserName;

    @Value("${webdriver.version}")
    private String webdriverVersion;

    @Value("${webdriver.platform}")
    private String webdriverPlatform;

    @Value("${webdriver.sessionId}")
    private String webdriverSessionId;

    @Value("${webdriver.session.message}")
    private String webdriverSessionMessage;

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

}
