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

import io.elastest.eus.session.SessionManager;

/**
 * Utility class for serialize JSON messages (recorded session).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public class WebSocketRecordedSession {

    WebSocketSessionInfoEntry recordedSession;

    public WebSocketRecordedSession() {
        // Empty default construct (needed by Jackson)
    }

    public WebSocketRecordedSession(SessionManager sessionManager) {
        recordedSession = new WebSocketSessionInfoEntry(sessionManager);
    }

    public WebSocketSessionInfoEntry getRecordedSession() {
        return recordedSession;
    }

    @Override
    public String toString() {
        return "WebSocketRecordedSession [getRecordedSession()="
                + getRecordedSession() + "]";
    }

}
