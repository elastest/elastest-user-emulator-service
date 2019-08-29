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

import io.elastest.eus.test.base.EusBaseTest;
import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumExtension;

/**
 * E2E ETM test.
 *
 * @author EduJG(https://github.com/EduJGURJC)
 * @since 0.1.1
 */
@Tag("e2e")
@DisplayName("EUS E2E tests using AWS Browsers")
@ExtendWith(SeleniumExtension.class)
public class EusAWSBrowserE2eTest extends EusBaseTest {
    final String sutName = "Example.org";
    final int timeout = 600;

    String tJobImage = "elastest/test-etm-alpinegitjava";
    String tJobMultiBrowserTestResultPath = "/demo-projects/example-org-test/target/surefire-reports/";

    private static final Map<String, List<String>> tssMap;
    static {
        tssMap = new HashMap<String, List<String>>();
        tssMap.put("EUS", Arrays.asList("webRtcStats"));
    }

    void createProjectAndSut(WebDriver driver) throws Exception {
        navigateToTorm(driver);
        if (!etProjectExists(driver, PROJECT_NAME)) {
            createNewETProject(driver, PROJECT_NAME);
        }
        if (!etSutExistsIntoProject(driver, PROJECT_NAME, sutName)) {
            // Create SuT
            String sutDesc = "Example web";
            createNewSutDeployedOutsideWithManualInstrumentation(driver,
                    sutName, sutDesc, "example.org", null);
        }

    }

    @Test
    @DisplayName("Create Example.org Test")
    void testBrowserInAWSTest(
            @DockerBrowser(type = CHROME) RemoteWebDriver localDriver,
            TestInfo testInfo) throws Exception {
        setupTestBrowser(testInfo, CHROME, localDriver);

        this.createProjectAndSut(driver);
        navigateToETProject(driver, PROJECT_NAME);

        // AWS Params
        Map<String, String> params = new HashMap<>();
        Map<String, String> envs = System.getenv();
        if (envs != null) {
            params.put("USE_AWS_BROWSER", "true");
            for (HashMap.Entry<String, String> env : envs.entrySet()) {
                if (env != null && env.getKey().startsWith("AWS_")) {
                    params.put(env.getKey(), env.getValue());
                }
            }
        }

        String tJobName = "Example.org Test";
        if (!etTJobExistsIntoProject(driver, PROJECT_NAME, tJobName)) {
            String commands = "git clone https://github.com/elastest/demo-projects; cd /demo-projects/example-org-test; mvn -B -Dbrowser=chrome test;";

            createNewTJob(driver, tJobName, tJobMultiBrowserTestResultPath,
                    sutName, tJobImage, false, commands, params, tssMap, null,
                    null, true);
        }
        // Run TJob
        runTJobFromProjectPage(driver, tJobName, true, null);

        this.checkFinishTJobExec(driver, timeout, "SUCCESS", false);

        // Check the presence of testCaseInfo
        getElementById(driver, "testCaseInfo", 20);
    }

}
