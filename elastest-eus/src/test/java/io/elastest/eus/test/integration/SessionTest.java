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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.api.service.JsonService;
import io.elastest.eus.app.EusSpringBootApp;

/**
 * Session test (including VNC).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EusSpringBootApp.class, webEnvironment = RANDOM_PORT)
public class SessionTest {

    final Logger log = LoggerFactory.getLogger(SessionTest.class);

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JsonService jsonService;

    @LocalServerPort
    int serverPort;

    @BeforeEach
    void setup() {
        log.debug("App started on port {}", serverPort);
    }

    @Test
    void createAndDestroySession() {
        // Test data (input)
        String jsonMessage = "{\n" + "    \"desiredCapabilities\": {\n"
                + "        \"browserName\": \"chrome\",\n"
                + "        \"version\": \"\",\n"
                + "        \"platform\": \"ANY\"\n" + "    }\n" + "}";

        // Exercise #1 (create session)
        log.debug("POST /session");
        ResponseEntity<String> response = restTemplate.postForEntity("/session",
                jsonMessage, String.class);

        // Test outcome #1 (output)
        HttpStatus statusCode = response.getStatusCode();
        String responseBody = response.getBody();
        log.debug("[POST /session] Status code {}", statusCode);
        log.debug("[POST /session] Response {}", responseBody);

        String sessionId = jsonService.getSessionIdFromResponse(responseBody);
        log.debug("sessionId {}", sessionId);

        // Assertions #1
        assertEquals(OK, response.getStatusCode());
        assertNotNull(sessionId);

        // ---------------

        // Exercise #2 (get VNC session)
        log.debug("GET /session/{}/vnc", sessionId);
        response = restTemplate.getForEntity("/session/" + sessionId + "/vnc",
                String.class);

        // Test outcome #2 (output)
        statusCode = response.getStatusCode();
        responseBody = response.getBody();
        log.debug("[GET /session/{}/vnc] Status code {}", sessionId,
                statusCode);
        log.debug("[GET /session/{}/vnc] Response {}", sessionId, responseBody);

        // ---------------

        // Assertions #2
        assertEquals(OK, response.getStatusCode());
        assertNotNull(sessionId);

        // Exercise #3 (destroy session)
        log.debug("DELETE /session/{}", sessionId);
        restTemplate.delete("/session/" + sessionId);

        // TODO this can be done as an scenario test when available (JUnit 5 M6)
    }

}
