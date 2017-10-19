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

import java.util.List;

/**
 * Utility class for serialize JSON log responses.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.0-alpha2
 */
public class WebDriverLog {

    String sessionId;
    int status;
    List<Value> value;

    public String getSessionId() {
        return sessionId;
    }

    public int getStatus() {
        return status;
    }

    public List<Value> getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "WebDriverLog [getSessionId()=" + getSessionId()
                + ", getStatus()=" + getStatus() + ", getValue()=" + getValue()
                + "]";
    }

    public static class Value {
        String level;
        String message;

        public String getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "[" + getLevel() + "] " + getMessage();
        }
    }

}
