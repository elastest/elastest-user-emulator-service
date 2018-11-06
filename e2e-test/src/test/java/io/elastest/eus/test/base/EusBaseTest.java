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
package io.elastest.eus.test.base;

import static java.lang.System.getProperty;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.logging.Level.ALL;
import static org.openqa.selenium.logging.LogType.BROWSER;
import static org.openqa.selenium.remote.CapabilityType.LOGGING_PREFS;
import static org.openqa.selenium.remote.DesiredCapabilities.chrome;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import io.github.bonigarcia.DriverCapabilities;

/**
 * Parent for E2E EUS tests.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public class EusBaseTest {

    final Logger log = getLogger(lookup().lookupClass());

    protected String tormUrl = "http://172.17.0.1:37000/"; // local by default
    protected String tormOriginalUrl = tormUrl;
    protected String eUser = null;
    protected String ePassword = null;
    protected static String eusURL = null;
    protected boolean secureElastest = false;
    public static final String CHROME_BROWSER = "chrome";

    protected WebDriver driver;

    @DriverCapabilities
    DesiredCapabilities capabilities = chrome();
    {
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(BROWSER, ALL);
        capabilities.setCapability(LOGGING_PREFS, logPrefs);
    }

    @BeforeEach
    void setup() {
        String etmApi = getProperty("eEtmApi");
        if (etmApi != null) {
            tormUrl = etmApi;
        }
        String elastestUser = getProperty("eUser");
        eusURL = System.getenv("ET_EUS_API");

        if (elastestUser != null) {
            eUser = elastestUser;

            String elastestPassword = getProperty("ePass");
            if (elastestPassword != null) {
                ePassword = elastestPassword;
                secureElastest = true;
            }
        }
        
        if (secureElastest) {
            tormOriginalUrl = tormUrl;
            String split_url[] = tormUrl.split("//");
            tormUrl = split_url[0] + "//" + eUser + ":" + ePassword + "@"
                    + split_url[1];
        }

        log.info("Using URL {} to connect to {} TORM", tormUrl,
                secureElastest ? "secure" : "unsecure");
    }

    @AfterEach
    void teardown() throws IOException {
        if (driver != null) {
            log.info("Browser console at the end of the test");
            LogEntries logEntries = driver.manage().logs().get(BROWSER);
            logEntries.forEach((entry) -> log.info("[{}] {} {}",
                    new Date(entry.getTimestamp()), entry.getLevel(),
                    entry.getMessage()));
            if (eusURL != null) {
                try {
                    driver.close();
                } catch (UnreachableBrowserException ube) {
                    log.error("Error trying to close the browser session.");
                }
            }
        }
    }

    protected void createNewProject(WebDriver driver, String projectName) {
        driver.findElement(
                By.xpath("//button[contains(string(), 'New Project')]"))
                .click();
        driver.findElement(By.name("project.name")).sendKeys(projectName);
        driver.findElement(By.xpath("//button[contains(string(), 'SAVE')]"))
                .click();
    }

    protected void startTestSupportService(WebDriver driver,
            String supportServiceLabel) {
        WebElement tssNavButton = driver
                .findElement(By.id("nav_support_services"));
        if (!tssNavButton.isDisplayed()) {
            driver.findElement(By.id("main_menu")).click();
        }
        tssNavButton.click();

        WebDriverWait waitElement = new WebDriverWait(driver, 3);
        By supportService;
        int numRetries = 1;
        do {
            driver.findElement(By.className("mat-select-trigger")).click();
            supportService = By.xpath("//md-option[contains(string(), '"
                    + supportServiceLabel + "')]");
            try {
                waitElement.until(visibilityOfElementLocated(supportService));
                log.info("Element {} already available", supportService);
                break;

            } catch (Exception e) {
                numRetries++;
                if (numRetries > 6) {
                    log.warn("Max retries ({}) reached ... leaving",
                            numRetries);
                    break;
                }
                log.warn("Element {} not available ... retrying",
                        supportService);
            }
        } while (true);
        driver.findElement(supportService).click();

        log.info("Create and wait instance");
        driver.findElement(By.id("create_instance")).click();
        WebDriverWait waitService = new WebDriverWait(driver, 120); // seconds
        By serviceDetailButton = By
                .xpath("//button[@title='View Service Detail']");
        waitService.until(visibilityOfElementLocated(serviceDetailButton));
        driver.findElement(serviceDetailButton).click();
    }
    
    public void selectOptionFromSelect(String option) {
        WebDriverWait waitElement = new WebDriverWait(driver, 3);
        By select;
        int numRetries = 1;
        do {
            driver.findElement(By.className("mat-select-trigger")).click();
            select = By.xpath("//md-option[contains(string(), '"
                    + option + "')]");
            try {
                waitElement.until(visibilityOfElementLocated(select));
                log.info("Element {} already available", select);
                break;

            } catch (Exception e) {
                numRetries++;
                if (numRetries > 6) {
                    log.warn("Max retries ({}) reached ... leaving",
                            numRetries);
                    break;
                }
                log.warn("Element {} not available ... retrying",
                        select);
            }
        } while (true);
        driver.findElement(select).click();
    }
    
    public void setupTest(String testName) throws MalformedURLException {
        DesiredCapabilities caps;
        caps = eusURL != null ? DesiredCapabilities.firefox()
                : DesiredCapabilities.chrome();
        caps.setCapability("testName", testName);
        driver = new RemoteWebDriver(new URL(eusURL), caps);
    }

}
