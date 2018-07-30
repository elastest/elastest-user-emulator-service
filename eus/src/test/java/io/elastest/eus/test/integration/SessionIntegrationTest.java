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

import static java.lang.invoke.MethodHandles.lookup;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.json.WebDriverSessionResponse;
import io.elastest.eus.service.EusJsonService;
import io.elastest.eus.test.util.WebSocketClient;
import io.elastest.eus.test.util.WebSocketClient.MessageHandler;

/**
 * Session test (including VNC).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration tests with W3C WebDriver sessions")
public class SessionIntegrationTest {

    final Logger log = getLogger(lookup().lookupClass());

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    EusJsonService jsonService;

    @LocalServerPort
    int serverPort;

    @Value("${ws.path}")
    private String wsPath;

    @Value("${api.context.path}")
    private String apiContextPath;

    @BeforeEach
    void setup() {
        log.debug("App started on port {}", serverPort);
    }

    @Test
    @DisplayName("Create and destroy session")
    void createAndDestroySession() throws Exception {
        // #1 Create session and WebSocket connection
        String wsUrl = "ws://localhost:" + serverPort + wsPath;
        log.debug("Websocket url: {}", wsUrl);
        WebSocketClient webSocketClient = new WebSocketClient(wsUrl);
        webSocketClient.addMessageHandler(new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                log.debug("Received message: {}", message);
                assertThat(message, jsonService.isJsonValid(message));
            }
        });

        log.debug("POST /session");
        String firefoxVersion = "59.0";
        String jsonMessage = "{\"capabilities\":{\"alwaysMatch\":{\"acceptInsecureCerts\":true},"
                + "\"desiredCapabilities\":{\"acceptInsecureCerts\":true,\"browserName\":\"firefox\","
                + "\"platform\":\"ANY\"," + "\"version\":\"" + firefoxVersion
                + "\",\"loggingPrefs\":{\"browser\":\"ALL\"}},"
                + "\"firstMatch\":[{\"browserName\":\"firefox\"}],\"requiredCapabilities\":{}},"
                + "\"desiredCapabilities\":{\"acceptInsecureCerts\":true,\"browserName\":\"firefox\","
                + "\"platform\":\"ANY\",\"version\":\"" + firefoxVersion
                + "\",\"loggingPrefs\":{\"browser\":\"ALL\"}},"
                + "\"requiredCapabilities\":{}}";
        ResponseEntity<String> response = restTemplate.postForEntity(
                apiContextPath + "/session", jsonMessage, String.class);
        assertEquals(OK, response.getStatusCode());

        HttpStatus statusCode = response.getStatusCode();
        String responseBody = response.getBody();
        log.debug("[POST /session] Status code {}", statusCode);
        log.debug("[POST /session] Response {}", responseBody);

        String sessionId = jsonService
                .jsonToObject(responseBody, WebDriverSessionResponse.class)
                .getSessionId();
        log.debug("sessionId {}", sessionId);
        assertNotNull(sessionId);

        // #2 Get VNC session)
        log.debug("GET /session/{}/vnc", sessionId);
        response = restTemplate.getForEntity(
                apiContextPath + "/session/" + sessionId + "/vnc",
                String.class);

        statusCode = response.getStatusCode();
        responseBody = response.getBody();
        log.debug("[GET /session/{}/vnc] Status code {}", sessionId,
                statusCode);
        log.debug("[GET /session/{}/vnc] Response {}", sessionId, responseBody);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(sessionId);

        // #3 Handle recordings
        log.debug("GET /session/{}/recording", sessionId);
        response = restTemplate.getForEntity(
                apiContextPath + "/session/" + sessionId + "/recording",
                String.class);

        assertEquals(OK, response.getStatusCode());
        assertThat(response.getHeaders().getContentType().toString(),
                containsString(TEXT_PLAIN_VALUE));

        log.debug("DELETE /session/{}/recording", sessionId);
        restTemplate.delete(
                apiContextPath + "/session/" + sessionId + "/recording");

        // Exercise #4 Destroy session and close WebSocket
        log.debug("DELETE /session/{}", sessionId);
        restTemplate.delete(apiContextPath + "/session/" + sessionId);
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
