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
package io.elastest.eus.test.api;

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
 * Session test.
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
        log.debug("POST /session");

        // Test data (input)
        String jsonMessage = "{\n" + "    \"desiredCapabilities\": {\n"
                + "        \"browserName\": \"chrome\",\n"
                + "        \"version\": \"\",\n"
                + "        \"platform\": \"ANY\"\n" + "    }\n" + "}";

        ResponseEntity<String> response = restTemplate.postForEntity("/session",
                jsonMessage, String.class);

        HttpStatus statusCode = response.getStatusCode();
        String responseBody = response.getBody();
        String sessionId = jsonService.getSessionId(responseBody);

        log.debug("Status code {}", statusCode);
        log.debug("Response {}", responseBody);
        log.debug("sessionId {}", sessionId);

        // Assertions
        assertEquals(OK, response.getStatusCode());
        assertNotNull(sessionId);

        log.debug("DELETE /session/{}", sessionId);
        restTemplate.delete("/session/" + sessionId);
    }

}
