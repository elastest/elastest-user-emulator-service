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

import static io.github.bonigarcia.BrowserType.CHROME;
import static java.lang.invoke.MethodHandles.lookup;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import io.elastest.eus.test.base.EusBaseTest;
import io.github.bonigarcia.BrowserType;
import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumExtension;

/**
 * Check that the EUS works properly together with a TJob. Requirements tested:
 * EUS1, EUS6, EUS9
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Tag("e2e")
@DisplayName("E2E tests of EUS through TORM")
@ExtendWith(SeleniumExtension.class)
public class EusTJobE2eTest extends EusBaseTest {
    final Logger log = getLogger(lookup().lookupClass());

    private static final Map<String, List<String>> tssMap;
    static {
        tssMap = new HashMap<String, List<String>>();
        tssMap.put("EUS", Arrays.asList("webRtcStats"));
    }

    void createProject(WebDriver driver) throws Exception {
        navigateToTorm(driver);
        if (!etProjectExists(driver, PROJECT_NAME)) {
            createNewETProject(driver, PROJECT_NAME);
        }
    }

    @Test
    @DisplayName("EUS in a TJob")
    void testTJob(@DockerBrowser(type = CHROME) RemoteWebDriver localDriver,
            TestInfo testInfo) throws Exception {
        setupTestBrowser(testInfo, BrowserType.CHROME, localDriver);

        // Setting up the TJob used in the test
        this.createProject(driver);
        navigateToETProject(driver, PROJECT_NAME);
        String tJobName = "eus-test-tjob";
        if (!etTJobExistsIntoProject(driver, PROJECT_NAME, tJobName)) {
            String tJobTestResultPath = "/home/jenkins/elastest-user-emulator-service/tjob-test/target/surefire-reports/TEST-io.elastest.eus.test.e2e.TJobEusTest.xml";
            String sutName = null;
            String tJobImage = "elastest/ci-docker-e2e";
            String commands = "git clone https://github.com/elastest/elastest-user-emulator-service; cd elastest-user-emulator-service/tjob-test; mvn test;";
            createNewTJob(driver, tJobName, tJobTestResultPath, sutName,
                    tJobImage, false, commands, null, tssMap, null, 5);
        }
        // Run the TJob
        runTJobFromProjectPage(driver, tJobName);

        // Wait for eus card
        WebDriverWait waitEus = new WebDriverWait(driver, 60);
        By eusCard = By.xpath("//mat-card-title[contains(string(), 'EUS')]");
        waitEus.until(visibilityOfElementLocated(eusCard));

        // and check its result
        this.checkFinishTJobExec(driver, 220, "SUCCESS", false);
    }
}
