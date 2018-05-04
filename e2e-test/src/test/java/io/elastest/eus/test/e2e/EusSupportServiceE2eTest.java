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
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openqa.selenium.Keys.RETURN;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import io.elastest.eus.test.base.EusBaseTest;
import io.github.bonigarcia.DockerBrowser;
import io.github.bonigarcia.SeleniumExtension;

/**
 * E2E EUS test.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Tag("e2e")
@DisplayName("E2E tests of EUS through TORM")
@ExtendWith(SeleniumExtension.class)
public class EusSupportServiceE2eTest extends EusBaseTest {

    final Logger log = getLogger(lookup().lookupClass());

    @Test
    @DisplayName("EUS as support service")
    void testSupportService(
            @DockerBrowser(type = CHROME) RemoteWebDriver driver)
            throws InterruptedException {
        this.driver = driver;

        log.info("Navigate to TORM and start support service");
        driver.manage().window().setSize(new Dimension(1024, 1024));
        driver.manage().timeouts().implicitlyWait(5, SECONDS); // implicit wait
        if (secureElastest) {
            driver.get(secureTorm);
        } else {
            driver.get(tormUrl);
        }
        startTestSupportService(driver, "EUS");

        log.info("Select Chrome as browser and start session");
        WebDriverWait waitElement = new WebDriverWait(driver, 40); // seconds
        By chromeRadioButton = By.id("chrome_radio");
        waitElement.until(visibilityOfElementLocated(chromeRadioButton));
        driver.findElement(chromeRadioButton).click();
        driver.findElement(By.id("start_session")).click();

        log.info("Wait to load browser");
        By eusBrowser = By.id("eusBrowser");
        WebDriverWait waitBrowser = new WebDriverWait(driver, 240); // seconds
        waitBrowser.until(visibilityOfElementLocated(eusBrowser));
        driver.findElement(eusBrowser).click();

        log.info("Click browser navigation bar and navigate");
        By vncCanvas = By.id("vnc_canvas");
        WebElement canvas = driver.findElement(vncCanvas);
        waitElement.until(visibilityOfElementLocated(vncCanvas));
        sleep(SECONDS.toMillis(2));
        new Actions(driver).moveToElement(canvas, 80, 16).click()
                .sendKeys("elastest.io" + RETURN).build().perform();
        int navigationTimeSec = 5;
        log.info("Waiting {} seconds (simulation of manual navigation)",
                navigationTimeSec);
        sleep(SECONDS.toMillis(navigationTimeSec));

        log.info("Close browser and wait to dispose canvas");
        driver.switchTo().defaultContent();
        driver.findElement(By.id("close_dialog")).click();
        waitElement.until(invisibilityOfElementLocated(
                By.cssSelector("md-dialog-container")));

        log.info("View recording");
        driver.findElement(By.id("view_recording")).click();
        sleep(SECONDS.toMillis(navigationTimeSec));
        driver.findElement(By.id("close_dialog")).click();
        waitElement.until(invisibilityOfElementLocated(
                By.cssSelector("md-dialog-container")));

        log.info("Delete recording");
        By deleteRecording = By.id("delete_recording");
        driver.findElement(deleteRecording).click();
        waitElement.until(invisibilityOfElementLocated(deleteRecording));
    }

}