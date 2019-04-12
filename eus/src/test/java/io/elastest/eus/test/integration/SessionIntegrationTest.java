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
package io.elastest.eus.test.integration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import io.elastest.eus.test.BaseTest;
import io.elastest.eus.test.util.WebSocketClient;
import io.elastest.eus.test.util.WebSocketClient.MessageHandler;

/**
 * Session test (including VNC).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Tag("integration")
@DisplayName("Integration tests with W3C WebDriver sessions")
public class SessionIntegrationTest extends BaseTest {

    @BeforeEach
    void setup() {
        log.debug("App started on port {}", serverPort);
    }

    @Test
    @DisplayName("Create and destroy session")
    void createAndDestroySession() throws Exception {
        // #1 Create WebSocket connection
        WebSocketClient webSocketClient = createWebSocket();
        webSocketClient.addMessageHandler(new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                log.debug("Received message: {}", message);
                assertThat(message, jsonService.isJsonValid(message));
            }
        });

        // #2 Create Session
        log.debug("POST /session");
        String firefoxVersion = "latest";

        ResponseEntity<String> response = this.startSession("firefox",
                firefoxVersion);
        assertEquals(OK, response.getStatusCode());

        HttpStatus statusCode = response.getStatusCode();
        String responseBody = response.getBody();
        log.debug("[POST /session] Status code {}", statusCode);
        log.debug("[POST /session] Response {}", responseBody);

        String sessionId = this.getSessionIdFromResponse(response);
        log.debug("sessionId {}", sessionId);
        assertNotNull(sessionId);

        // #3 Get VNC session)
        log.debug("GET /session/{}/vnc", sessionId);
        response = this.getVncSession(sessionId);

        statusCode = response.getStatusCode();
        responseBody = response.getBody();
        log.debug("[GET /session/{}/vnc] Status code {}", sessionId,
                statusCode);
        log.debug("[GET /session/{}/vnc] Response {}", sessionId, responseBody);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(sessionId);

        // #4 Handle recordings
        log.debug("GET /session/{}/recording", sessionId);
        response = this.getRecordings(sessionId);

        assertEquals(OK, response.getStatusCode());
        assertThat(response.getHeaders().getContentType().toString(),
                containsString(TEXT_PLAIN_VALUE));

        log.debug("DELETE /session/{}/recording", sessionId);
        this.deleteSessionRecordings(sessionId);

        // #5 Upload a file
        log.debug("POST /browserfile/session/{}", sessionId);

        String fileName = sessionId + "_upload_file.txt";
        MultipartFile file = getMultipartFileFromString(fileName,
                "Hello World!");
        response = uploadFileToSession(file, sessionId);
        log.debug("Response body: {}", response.getBody());
        assertEquals(OK, response.getStatusCode());
        assertThat(response.getHeaders().getContentType().toString(),
                containsString(TEXT_PLAIN_VALUE));

        // #6 Get file from session
        log.debug("GET /browserfile/session/{}/{}", sessionId,
                "PATH/" + fileName);

        ResponseEntity<InputStreamResource> responseFile = this
                .getUploadedFileFromSession("aaa", sessionId);
        assertEquals(OK, responseFile.getStatusCode());
        assertThat(responseFile.getHeaders().getContentType().toString(),
                containsString(TEXT_PLAIN_VALUE));

        // #7 Destroy session and close WebSocket
        log.debug("DELETE /session/{}", sessionId);
        this.deleteSession(sessionId);
        webSocketClient.closeSession();

        // NOTE: This can be done as an scenario test when available (JUnit 5.1)
    }

    @Test
    @DisplayName("Get VNC URL of a non-valid session")
    void notFoundSession() {
        log.debug("GET /session/nofound/vnc");
        ResponseEntity<String> response = restTemplate.getForEntity(
                apiContextPath + "/session/nofound/vnc", String.class);

        assertEquals(NOT_FOUND, response.getStatusCode());
    }

}
