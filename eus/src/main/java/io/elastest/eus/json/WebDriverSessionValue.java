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

/**
 * Utility class for serialize JSON messages (session response with value).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public class WebDriverSessionValue {

    WebDriverSessionValueEntry value;

    public WebDriverSessionValue() {
        // Empty default construct (needed by Jackson)
    }

    public WebDriverSessionValueEntry getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "WebDriverSessionValue [getValue()=" + getValue() + "]";
    }

    public class WebDriverSessionValueEntry {
        String sessionId;
        Object capabilities;

        public WebDriverSessionValueEntry() {
            // Empty default construct (needed by Jackson)
        }

        public String getSessionId() {
            return sessionId;
        }

        public Object getCapabilities() {
            return capabilities;
        }

        @Override
        public String toString() {
            return "WebDriverSessionValueEntry [getSessionId()="
                    + getSessionId() + ", getCapabilities()="
                    + getCapabilities() + "]";
        }

    }

}
