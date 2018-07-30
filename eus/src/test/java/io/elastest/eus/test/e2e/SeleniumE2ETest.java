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
package io.elastest.eus.test.e2e;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openqa.selenium.remote.DesiredCapabilities.chrome;
import static org.openqa.selenium.remote.DesiredCapabilities.firefox;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Selenium test.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("e2e")
@DisplayName("End-to-end tests using Selenium WebDriver")
public class SeleniumE2ETest {

    final Logger log = getLogger(lookup().lookupClass());

    WebDriver driver;

    @LocalServerPort
    int serverPort;

    @Value("${api.context.path}")
    String apiContextPath;

    static Stream<Arguments> capabilitiesProvider() {
        return Stream.of(Arguments.of(chrome()), Arguments.of(firefox()));
    }

    @ParameterizedTest(name = "Using {0}")
    @DisplayName("Visit elastest.io using a browser provided by EUS")
    @MethodSource("capabilitiesProvider")
    void test(DesiredCapabilities capability) throws MalformedURLException {
        String eusUrl = "http://localhost:" + serverPort + apiContextPath;
        String sutUrl = "http://elastest.io/";

        log.debug("EUS URL: {}", eusUrl);
        log.debug("SUT URL: {}", sutUrl);

        driver = new RemoteWebDriver(new URL(eusUrl), capability);
        driver.get(sutUrl);

        String title = driver.getTitle();
        log.debug("SUT title: {}", title);
        assertEquals(title, "ElasTest Home");
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

}
