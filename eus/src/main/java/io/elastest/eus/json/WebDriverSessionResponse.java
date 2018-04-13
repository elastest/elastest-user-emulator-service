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
package io.elastest.eus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Utility class for serialize JSON messages (remove session).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public class WebDriverSessionResponse {

    String sessionId;
    int status;
    Value value;
    Object os;
    Object state;
    Object hCode;
    String hubContainerName;

    @JsonProperty("class")
    Object clazz;

    public WebDriverSessionResponse() {
        // Empty default construct (needed by Jackson)
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId != null ? sessionId : value.getSessionId();
    }

    public int getStatus() {
        return status;
    }

    public Object getValue() {
        return value;
    }

    public Object getOs() {
        return os;
    }

    public Object getState() {
        return state;
    }

    public Object gethCode() {
        return hCode;
    }

    public Object getClazz() {
        return clazz;
    }

    public String getHubContainerName() {
        return hubContainerName;
    }

    @Override
    public String toString() {
        return "WebDriverSessionResponse [getSessionId()=" + getSessionId()
                + ", getStatus()=" + getStatus() + ", getValue()=" + getValue()
                + ", getOs()=" + getOs() + ", getState()=" + getState()
                + ", gethCode()=" + gethCode() + ", getClazz()=" + getClazz()
                + ", getHubContainerName()=" + getHubContainerName() + "]";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Value {
        String sessionId;
        Object capabilities;

        public Value() {
            // Empty default construct (needed by Jackson)
        }

        public String getSessionId() {
            return sessionId;
        }
    }

}
