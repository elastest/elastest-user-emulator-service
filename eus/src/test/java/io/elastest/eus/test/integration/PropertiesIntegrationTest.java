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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.service.JsonService;
import io.elastest.eus.service.PropertiesService;

/**
 * Tests for properties service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration test for Properties Service")
public class PropertiesIntegrationTest {

    final Logger log = getLogger(lookup().lookupClass());

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
                Arguments.of("chrome", null, null, "chrome_62_LINUX"),
                Arguments.of("firefox", null, null, "firefox_57_LINUX"),
                Arguments.of("chrome", "58", null, "chrome_58_LINUX"),
                Arguments.of("firefox", "53", null, "firefox_53_LINUX"),
                Arguments.of("chrome", "", "", "chrome_62_LINUX"),
                Arguments.of("chrome", "", "ANY", "chrome_62_LINUX"),
                Arguments.of("firefox", "", "", "firefox_57_LINUX"),
                Arguments.of("firefox", "", "ANY", "firefox_57_LINUX"));
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
                        "selenium/standalone-chrome-debug:3.7.1"),
                Arguments.of("firefox", null, null,
                        "selenium/standalone-firefox-debug:3.7.1"),
                Arguments.of("chrome", "58", null,
                        "selenium/standalone-chrome-debug:3.4.0-chromium"),
                Arguments.of("firefox", "53", null,
                        "selenium/standalone-firefox-debug:3.4.0-dysprosium"));
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @DisplayName("Test properties key names")
    @MethodSource("keyProvider")
    void testKey(String browserName, String version, String platform,
            String expectedKey) {
        // Exercise service
        String realKey = propertiesService.getKeyFromCapabilities(browserName,
                version, platform);

        // Assertion
        assertEquals(expectedKey, realKey);
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @DisplayName("Test docker image names")
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
    @DisplayName("Invalid capabilities")
    void testInvalidCapabilities() {
        // Test data (input)
        String browserName = null;
        String version = null;
        String platform = null;

        // Exercise and assertion
        assertThrows(AssertionError.class, () -> {
            propertiesService.getKeyFromCapabilities(browserName, version,
                    platform);
        });
    }

    @Test
    @DisplayName("Valid capabilities")
    void testJson() throws IOException {
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
        String browserName = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getBrowserName();
        String version = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getVersion();
        String platform = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getPlatform();

        String realKey = propertiesService.getKeyFromCapabilities(browserName,
                version, platform);
        String realDocker = propertiesService
                .getDockerImageFromCapabilities(browserName, version, platform);

        // Assertions
        assertEquals(expectedKey, realKey);
        assertEquals(expectedDocker, realDocker);
    }

}
