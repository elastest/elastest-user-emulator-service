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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.api.EusException;
import io.elastest.eus.api.service.JsonService;
import io.elastest.eus.api.service.PropertiesService;
import io.elastest.eus.app.EusSpringBootApp;

/**
 * Tests for properties service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EusSpringBootApp.class)
public class PropertiesTest {

    final Logger log = LoggerFactory.getLogger(PropertiesTest.class);

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private JsonService jsonService;

    static Stream<Arguments> keyProvider() {
        return Stream.of(Arguments.of("chrome", "59", "ANY", "chrome_59_LINUX"),
                Arguments.of("chrome", "58", "LINUX", "chrome_58_LINUX"),
                Arguments.of("chrome", "57", "ANY", "chrome_57_LINUX"),
                Arguments.of("firefox", "54", "LINUX", "firefox_54_LINUX"),
                Arguments.of("firefox", "53", "ANY", "firefox_53_LINUX"),
                Arguments.of("firefox", "52", "LINUX", "firefox_52_LINUX"),
                Arguments.of("chrome", null, null, "chrome_59_LINUX"),
                Arguments.of("firefox", null, null, "firefox_54_LINUX"),
                Arguments.of("chrome", "58", null, "chrome_58_LINUX"),
                Arguments.of("firefox", "53", null, "firefox_53_LINUX"),
                Arguments.of("chrome", "", "", "chrome_59_LINUX"),
                Arguments.of("chrome", "", "ANY", "chrome_59_LINUX"),
                Arguments.of("firefox", "", "", "firefox_54_LINUX"),
                Arguments.of("firefox", "", "ANY", "firefox_54_LINUX"));
    }

    static Stream<Arguments> dockerProvider() {
        return Stream.of(
                Arguments.of("chrome", "59", "LINUX",
                        "selenium/standalone-chrome-debug:3.4.0-einsteinium"),
                Arguments.of("chrome", "58", "LINUX",
                        "selenium/standalone-chrome-debug:3.4.0-chromium"),
                Arguments.of("chrome", "57", "LINUX",
                        "selenium/standalone-chrome-debug:3.3.1"),
                Arguments.of("firefox", "54", "LINUX",
                        "selenium/standalone-firefox-debug:3.4.0-einsteinium"),
                Arguments.of("firefox", "53", "LINUX",
                        "selenium/standalone-firefox-debug:3.4.0-dysprosium"),
                Arguments.of("firefox", "52", "LINUX",
                        "selenium/standalone-firefox-debug:3.4.0-actinium"),
                Arguments.of("chrome", null, null,
                        "selenium/standalone-chrome-debug:3.4.0-einsteinium"),
                Arguments.of("firefox", null, null,
                        "selenium/standalone-firefox-debug:3.4.0-einsteinium"),
                Arguments.of("chrome", "58", null,
                        "selenium/standalone-chrome-debug:3.4.0-chromium"),
                Arguments.of("firefox", "53", null,
                        "selenium/standalone-firefox-debug:3.4.0-dysprosium"));
    }

    @ParameterizedTest
    @MethodSource("keyProvider")
    void testKey(String browserName, String version, String platform,
            String expectedKey) {
        // Exercise service
        String realKey = propertiesService.getKeyFromCapabilities(browserName,
                version, platform);

        // Assertion
        assertEquals(expectedKey, realKey);
    }

    @ParameterizedTest
    @MethodSource("dockerProvider")
    void testDocker(String browserName, String version, String platform,
            String expectedDocker) {
        // Exercise service
        String realDocker = propertiesService
                .getDockerImageFromCapabilities(browserName, version, platform);

        // Assertion
        assertEquals(expectedDocker, realDocker);
    }

    @Test
    void testException() {
        // Test data (input)
        String browserName = null;
        String version = null;
        String platform = null;

        // Exercise and assertion
        Throwable exception = assertThrows(EusException.class, () -> {
            propertiesService.getKeyFromCapabilities(browserName, version,
                    platform);
        });
        assertNotNull(exception.getMessage());

        log.debug("EusException raised when browserName is {}", browserName);
    }

    @Test
    void testJson() {
        // Test data (input)
        String jsonCapabilities = "{" + " \"desiredCapabilities\": {"
                + "\"browserName\": \"chrome\"," + " \"version\": \"59\","
                + "\"platform\": \"LINUX\"" + " },"
                + "\"requiredCapabilities\": {}," + " \"capabilities\": {"
                + "\"desiredCapabilities\": {" + "\"browserName\": \"chrome\","
                + " \"version\": \"59\"," + "\"platform\": \"LINUX\"" + " },"
                + "\"requiredCapabilities\": {}," + " \"alwaysMatch\": {},"
                + "\"firstMatch\": [" + " {" + "\"browserName\": \"chrome\""
                + " }" + " ]" + " }" + "}";

        // Expected data (outcome)
        String expectedKey = "chrome_59_LINUX";
        String expectedDocker = "selenium/standalone-chrome-debug:3.4.0-einsteinium";

        // Exercise
        String browserName = jsonService.getBrowser(jsonCapabilities);
        String version = jsonService.getVersion(jsonCapabilities);
        String platform = jsonService.getPlatform(jsonCapabilities);

        String realKey = propertiesService.getKeyFromCapabilities(browserName,
                version, platform);
        String realDocker = propertiesService
                .getDockerImageFromCapabilities(browserName, version, platform);

        // Assertions
        assertEquals(expectedKey, realKey);
        assertEquals(expectedDocker, realDocker);
    }

}
