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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openqa.selenium.remote.DesiredCapabilities.chrome;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.EusSpringBootApp;

/**
 * Timeout test.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EusSpringBootApp.class, webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = { "hub.timeout=5" })
public class TimeoutTest {

    final Logger log = LoggerFactory.getLogger(TimeoutTest.class);

    WebDriver driver;

    @LocalServerPort
    int serverPort;

    @Value("${api.context.path}")
    String apiContextPath;

    @BeforeEach
    void setup() throws MalformedURLException {
        String eusUrl = "http://localhost:" + serverPort + apiContextPath;
        driver = new RemoteWebDriver(new URL(eusUrl), chrome());
        log.debug("EUS URL: {}", eusUrl);
    }

    @Test
    void testTimeout() throws InterruptedException {
        long waitSeconds = 20;
        log.debug("Waiting {} seconds to force timeout", waitSeconds);
        SECONDS.sleep(waitSeconds);

        Throwable exception = assertThrows(WebDriverException.class,
                () -> driver.get("http://elastest.io/"));
        log.debug("Exception {} -- due to timeout", exception.getMessage());
        driver = null;
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

}
