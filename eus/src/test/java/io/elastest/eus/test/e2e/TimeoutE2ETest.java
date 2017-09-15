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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.net.URL;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Timeout test.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("e2e")
@DisplayName("End-to-end tests for timeout")
@TestPropertySource(properties = { "hub.timeout=1",
        "novnc.image.id=elastest/eus-novnc" })
public class TimeoutE2ETest {

    final Logger log = LoggerFactory.getLogger(TimeoutE2ETest.class);

    WebDriver driver;

    @LocalServerPort
    int serverPort;

    @Value("${server.servlet.context-path}")
    String contextPath;

    @Test
    @DisplayName("Assert exception due to timeout")
    void testTimeout() {
        assertThrows(Exception.class, () -> {
            String eusUrl = "http://localhost:" + serverPort + contextPath;
            log.debug("EUS URL: {}", eusUrl);
            driver = new RemoteWebDriver(new URL(eusUrl),
                    DesiredCapabilities.chrome());

            // Make several consecutive requests waiting for the end of session
            for (int i = 0; i < 10; i++) {
                driver.get("http://elastest.io/");
                log.info(driver.getTitle());
            }

            driver.close();
        });
    }

}
