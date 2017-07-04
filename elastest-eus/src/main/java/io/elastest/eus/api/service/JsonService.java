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

import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * Service implementation for JSON utilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class JsonService {

    public static final String CAPABILITES = "desiredCapabilities";
    public static final String BROWSERNAME = "browserName";
    public static final String VERSION = "version";
    public static final String PLATFORM = "platform";
    public static final String SESSION_ID = "sessionId";

    private JSONObject getCapabilities(String jsonMessage) {
        return (JSONObject) string2Json(jsonMessage).get(CAPABILITES);
    }

    public String getBrowser(String jsonMessage) {
        return (String) getCapabilities(jsonMessage).get(BROWSERNAME);
    }

    public String getSessionId(String jsonMessage) {
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

}
