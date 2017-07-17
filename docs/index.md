# ElasTest User Emulator Service

The ElasTest User-emulator Service (EUS) is an ElasTest service that provides browsers for both manual interaction and automated interacion under the control of tests, by means of starting browsers in containers. To achieve the former, provides a web interface that allows users to interact with the browser; for the later, EUS provides an extension of the [W3C WebDriver](https://www.w3.org/TR/webdriver/) specification, and thus, it can used as server (or hub) for Selenium tests. The main objective is to provide features similar to that of [BrowserStack](https://www.browserstack.com/), or [Saucelabs](https://saucelabs.com/), but integrated with ElasTest platform under an open source license.

## Features

## How to run

In order to run the EUS in the local machine, the following commands should be executed. Notice that Docker should be installed before hand in the machine hosting the EUS.

```
git clone https://github.com/elastest/elastest-user-emulator-service
cd elastest-eus
mvn spring-boot:run
```

At this point the EUS should be up an running on the localhost, and thus we can use its URL (http://localhost:8080/eus/v1 by default) in Selenium tests. For example, as a JUnit test:


```java
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;


public class RemoteTest1 {

    private WebDriver driver;

    @Before
    public void setupTest() throws MalformedURLException {
        DesiredCapabilities capability = DesiredCapabilities.chrome();
        driver = new RemoteWebDriver(new URL("http://localhost:8080/eus/v1"),
                capability);
    }

    @After
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void test() {
        // My test
    }

}
```

Moreover, the EUS is integrated with the ElasTest TORM GUI. To use this GUI, this project should be cloned and executed, as follows:


```
git clone https://github.com/elastest/elastest-torm/
cd elastest-torm-gui
ng serve
```

At this point we can see the TORM using the EUS, by default in the URL http://localhost:4200/.

## Basic usage

## Development documentation

### Architecture

### Prerequisites

