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

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.test.context.TestPropertySource;

import io.elastest.eus.test.BaseTest;

/**
 * Timeout test.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@TestPropertySource(properties = { "hub.timeout=5" })
public class TimeoutTest extends BaseTest {
    WebDriver driver;

    @BeforeEach
    void setup() throws MalformedURLException {
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
