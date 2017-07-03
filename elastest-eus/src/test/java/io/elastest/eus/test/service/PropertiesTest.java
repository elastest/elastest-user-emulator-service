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
package io.elastest.eus.test.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.ObjectArrayArguments.create;

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

    static Stream<Arguments> keyProvider() {
        return Stream.of(create("chrome", "59", "LINUX", "chrome_59_LINUX"),
                create("chrome", "58", "LINUX", "chrome_58_LINUX"),
                create("chrome", "57", "LINUX", "chrome_57_LINUX"),
                create("firefox", "54", "LINUX", "firefox_54_LINUX"),
                create("firefox", "53", "LINUX", "firefox_53_LINUX"),
                create("firefox", "52", "LINUX", "firefox_52_LINUX"),
                create("chrome", null, null, "chrome_59_LINUX"),
                create("firefox", null, null, "firefox_54_LINUX"),
                create("chrome", "58", null, "chrome_58_LINUX"),
                create("firefox", "53", null, "firefox_53_LINUX"));
    }

    static Stream<Arguments> dockerProvider() {
        return Stream.of(
                create("chrome", "59", "LINUX",
                        "selenium/standalone-chrome-debug:3.4.0-einsteinium"),
                create("chrome", "58", "LINUX",
                        "selenium/standalone-chrome-debug:3.4.0-chromium"),
                create("chrome", "57", "LINUX",
                        "selenium/standalone-chrome-debug:3.3.1"),
                create("firefox", "54", "LINUX",
                        "selenium/standalone-firefox-debug:3.4.0-einsteinium"),
                create("firefox", "53", "LINUX",
                        "selenium/standalone-firefox-debug:3.4.0-dysprosium"),
                create("firefox", "52", "LINUX",
                        "selenium/standalone-firefox-debug:3.4.0-actinium"),
                create("chrome", null, null,
                        "selenium/standalone-chrome-debug:3.4.0-einsteinium"),
                create("firefox", null, null,
                        "selenium/standalone-firefox-debug:3.4.0-einsteinium"),
                create("chrome", "58", null,
                        "selenium/standalone-chrome-debug:3.4.0-chromium"),
                create("firefox", "53", null,
                        "selenium/standalone-firefox-debug:3.4.0-dysprosium"));
    }

    @ParameterizedTest
    @MethodSource(names = "keyProvider")
    void testKey(String browserName, String version, String platform,
            String expectedKey) {
        // Exercise service
        String realKey = propertiesService.getKeyFromCapabilities(browserName,
                version, platform);

        // Assertion
        assertEquals(expectedKey, realKey);
    }

    @ParameterizedTest
    @MethodSource(names = "dockerProvider")
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
        String jsonMessage = "{\n" + " \"desiredCapabilities\": {\n"
                + " \"browserName\": \"chrome\",\n" + " \"version\": \"59\",\n"
                + " \"platform\": \"LINUX\"\n" + " },\n"
                + " \"requiredCapabilities\": {},\n" + " \"capabilities\": {\n"
                + " \"desiredCapabilities\": {\n"
                + " \"browserName\": \"chrome\",\n" + " \"version\": \"59\",\n"
                + " \"platform\": \"LINUX\"\n" + " },\n"
                + " \"requiredCapabilities\": {},\n" + " \"alwaysMatch\": {},\n"
                + " \"firstMatch\": [\n" + " {\n"
                + " \"browserName\": \"chrome\"\n" + " }\n" + " ]\n" + " }\n"
                + "}";

        // Expected data (outcome)
        String expectedKey = "chrome_59_LINUX";
        String expectedDocker = "selenium/standalone-chrome-debug:3.4.0-einsteinium";

        // Exercise
        String realKey = propertiesService.getKeyFromJson(jsonMessage);
        String realDocker = propertiesService
                .getDockerImageFromJson(jsonMessage);

        // Assertions
        assertEquals(expectedKey, realKey);
        assertEquals(expectedDocker, realDocker);
    }

}
