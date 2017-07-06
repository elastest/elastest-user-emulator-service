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
package io.elastest.eus.test.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.api.service.JsonService;
import io.elastest.eus.app.EusSpringBootApp;

/**
 * Tests for properties JSON service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EusSpringBootApp.class)
public class JsonServiceTest {

    final Logger log = LoggerFactory.getLogger(JsonServiceTest.class);

    @Autowired
    private JsonService jsonService;

    @ParameterizedTest
    @ValueSource(strings = { "/session/4562f70d-a350-4e88-96da-25d56c91f336",
            "/session/a3a824f9-ee62-4f2d-ab73-1b883efa83d5",
            "/session/03e9d769-95e6-4772-939e-0bcd4d9d1b37/url", "/session",
            "/status" })
    void testKey(String path) {
        // Exercise service
        Optional<String> sessionId = jsonService.getSessionIdFromPath(path);

        int countCharsInString = jsonService.countCharsInString(path, '/');

        String errorMessage = "path " + path + " -- sessionId present "
                + sessionId.isPresent() + " -- countCharsInString "
                + countCharsInString;
        log.trace(errorMessage);

        assertTrue(
                (countCharsInString == 1 && !sessionId.isPresent())
                        || (countCharsInString > 1 && sessionId.isPresent()),
                errorMessage);
    }

}
