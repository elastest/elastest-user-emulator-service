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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.elastest.eus.test.base.EtmBaseTest;
import io.github.bonigarcia.BrowserType;
import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumExtension;

/**
 * E2E ETM test.
 *
 * @author EduJG(https://github.com/EduJGURJC)
 * @since 0.1.1
 */
@Tag("e2e")
@DisplayName("ETM E2E test of OpenVidu WebRTC project")
@ExtendWith(SeleniumExtension.class)
public class EusWebRtcE2eTest extends EtmBaseTest {
    final String projectName = "EUS E2E WebRTC";
    final String sutName = "OpenVidu Test App";
    final int timeout = 350;

    private static final Map<String, List<String>> tssMap;
    static {
        tssMap = new HashMap<String, List<String>>();
        tssMap.put("EUS", Arrays.asList("webRtcStats"));
    }

    void createProjectAndSut(WebDriver driver) throws Exception {
        navigateToTorm(driver);
        if (!etProjectExists(driver, projectName)) {
            createNewETProject(driver, projectName);
        }
        if (!etSutExistsIntoProject(driver, projectName, sutName)) {
            // Create SuT
            String sutDesc = "OpenVidu Description";
            String sutImage = "openvidu/testapp:elastest";
            String sutPort = "4443";
            createNewSutDeployedByElastestWithImage(driver, sutName, sutDesc,
                    sutImage, sutPort, null);
        }

    }

    @Test
    @DisplayName("Create OpenVidu WebRTC project Chrome Test")
    void testCreateOpenViduWebRTC(
            @DockerBrowser(type = CHROME) RemoteWebDriver localDriver,
            TestInfo testInfo) throws Exception {
        setupTestBrowser(testInfo, BrowserType.CHROME, localDriver);
        this.createProjectAndSut(driver);
        navigateToETProject(driver, projectName);

        String tJobName = "Videocall Test";
        if (!etTJobExistsIntoProject(driver, projectName, tJobName)) {
            String tJobTestResultPath = "/demo-projects/openvidu-test/target/surefire-reports/";
            String tJobImage = "elastest/test-etm-alpinegitjava";
            String commands = "echo \"Cloning project\"; git clone https://github.com/elastest/demo-projects; cd demo-projects/openvidu-test; echo \"Compiling project\"; mvn -DskipTests=true -B package; echo \"Executing test\"; mvn -B test;";

            createNewTJob(driver, tJobName, tJobTestResultPath, sutName,
                    tJobImage, false, commands, null, tssMap, null);
        }
        // Run TJob
        runTJobFromProjectPage(driver, tJobName);

        this.checkFinishTJobExec(driver, timeout, "SUCCESS", false);

        // Check WebRtc metrics
        openTJobExecMonitoringConfigModal(driver);

        String webRtcMetricsXpath = "//*[@id=\"metricsTree\"]//div/div/tree-node-collection/div/tree-node[1]/div/tree-node-children/div/tree-node-collection/div/tree-node/div/tree-node-wrapper/div/div//span/text()[contains(string(), 'et_dockbeat')]";
        assertNotNull(getElementByXpath(driver, webRtcMetricsXpath));
    }
}
