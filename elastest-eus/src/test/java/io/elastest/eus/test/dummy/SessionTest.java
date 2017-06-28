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
package io.elastest.eus.test.dummy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

    @LocalServerPort
    int serverPort;

    @Before
    void setup() {
        log.debug("[@Before] app started on port {}", serverPort);
    }

    @Test
    void test() {
        log.debug("[@Test] GET /session/{sessionId}/stats");

        String sessionId = new BigInteger(130, new SecureRandom()).toString(32);

        ResponseEntity<Object> response = restTemplate
                .getForEntity("/session/" + sessionId + "/stats", Object.class);
        log.debug("Response {}", response.getStatusCode());

        assertEquals(OK, response.getStatusCode());
    }

}
