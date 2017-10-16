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

import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openqa.selenium.Keys.RETURN;
import static org.openqa.selenium.OutputType.BASE64;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

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
public class EusSupportServiceE2eTest {

    final Logger log = getLogger(lookup().lookupClass());

    String tormUrl = "http://localhost:37006/"; // default value (local)

    @BeforeEach
    void setup() {
        String etmApi = getProperty("etEmpApi");
        if (etmApi != null) {
            tormUrl = etmApi;
        }
        log.info("Using URL {} to connect to TORM", tormUrl);
    }

    @Test
    @DisplayName("EUS as support service")
    void testSupportService(ChromeDriver driver) throws Exception {
        log.info("Navigate to TORM and start support service");
        driver.manage().window().setSize(new Dimension(1024, 1024));
        driver.manage().timeouts().implicitlyWait(5, SECONDS); // implicit wait
        driver.get(tormUrl);
        startTestSupportService(driver, "EUS");

        log.info("Select Chrome as browser and start session");
        driver.findElement(By.id("chrome_radio")).click();
        driver.findElement(By.id("start_session")).click();

        log.info("Wait to load browser");
        By iframe = By.id("eus_iframe");
        WebDriverWait waitBrowser = new WebDriverWait(driver, 120); // seconds
        waitBrowser.until(visibilityOfElementLocated(iframe));
        log.info("Canvas iframe URL {}",
                driver.findElement(iframe).getAttribute("src"));
        driver.switchTo().frame(driver.findElement(iframe));
        log.info("Click browser navigation bar and navigate");
        WebElement canvas = driver.findElement(By.id("noVNC_canvas"));
        new Actions(driver).moveToElement(canvas, 142, 45).click().build()
                .perform();
        canvas.sendKeys("elastest.io" + RETURN);
        int navigationTimeSec = 5;
        log.info("Waiting {} secons (simulation of manual navigation)",
                navigationTimeSec);
        sleep(SECONDS.toMillis(navigationTimeSec));
        log.info("Screenshot (in Base64) after manual navigation:\n{}",
                getBase64Screenshot(driver));

        log.info("Close browser and wait to dispose iframe");
        driver.switchTo().defaultContent();
        driver.findElement(By.id("close_dialog")).click();
        WebDriverWait waitElement = new WebDriverWait(driver, 30); // seconds
        waitElement.until(invisibilityOfElementLocated(
                By.cssSelector("md-dialog-container")));

        log.info("View recording and delete it");
        driver.findElement(By.id("view_recording")).click();
        sleep(SECONDS.toMillis(navigationTimeSec));
        log.info("Screenshot (in Base64) after view recording:\n{}",
                getBase64Screenshot(driver));
        driver.findElement(By.id("close_dialog")).click();
        waitElement.until(invisibilityOfElementLocated(
                By.cssSelector("md-dialog-container")));

        By deleteRecording = By.id("delete_recording");
        driver.findElement(deleteRecording).click();
        waitElement.until(invisibilityOfElementLocated(deleteRecording));
        log.info("Screenshot (in Base64) at the end of test:\n{}",
                getBase64Screenshot(driver));
    }

    void startTestSupportService(WebDriver driver, String supportServiceLabel) {
        WebElement tssNavButton = driver
                .findElement(By.id("nav_support_services"));
        if (!tssNavButton.isDisplayed()) {
            driver.findElement(By.id("main_menu")).click();
        }
        tssNavButton.click();
        driver.findElement(By.className("mat-select-trigger")).click();
        driver.findElement(By.xpath("//md-option[contains(string(), '"
                + supportServiceLabel + "')]")).click();

        log.info("Create and wait instance");
        driver.findElement(By.id("create_instance")).click();
        WebDriverWait waitService = new WebDriverWait(driver, 60); // seconds
        By serviceDetailButton = By
                .xpath("//button[@title='View Service Detail']");
        waitService.until(visibilityOfElementLocated(serviceDetailButton));
        driver.findElement(serviceDetailButton).click();
    }

    String getBase64Screenshot(WebDriver driver) throws IOException {
        String screenshotBase64 = ((TakesScreenshot) driver)
                .getScreenshotAs(BASE64);
        return "data:image/png;base64," + screenshotBase64;
    }

}
