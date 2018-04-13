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
package io.elastest.eus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Utility class for serialize JSON messages (remove session).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebDriverCapabilities {

    DesiredCapabilities desiredCapabilities;
    Object capabilities;
    Object requiredCapabilities;
    Object alwaysMatch;
    Object firstMatch;

    public WebDriverCapabilities() {
        // Empty default construct (needed by Jackson)
    }

    public WebDriverCapabilities(String browserName, String version,
            String platform) {
        desiredCapabilities = new DesiredCapabilities(browserName, version,
                platform);
    }

    public DesiredCapabilities getDesiredCapabilities() {
        return desiredCapabilities;
    }

    public Object getCapabilities() {
        return capabilities;
    }

    public Object getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public Object getAlwaysMatch() {
        return alwaysMatch;
    }

    public Object getFirstMatch() {
        return firstMatch;
    }

    @Override
    public String toString() {
        return "WebDriverCapabilities [getDesiredCapabilities()="
                + getDesiredCapabilities() + ", getCapabilities()="
                + getCapabilities() + ", getRequiredCapabilities()="
                + getRequiredCapabilities() + ", getAlwaysMatch()="
                + getAlwaysMatch() + ", getFirstMatch()=" + getFirstMatch()
                + "]";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class DesiredCapabilities {
        LoggingPrefs loggingPrefs;
        String browserName;

        @JsonProperty("firefox_profile")
        String firefoxProfile;

        Object chromeOptions;
        Object operaOptions;
        String version;
        String platform;
        boolean acceptInsecureCerts;
        boolean live;

        String browserId;
        boolean manualRecording;

        public DesiredCapabilities() {
            // Empty default construct
        }

        public DesiredCapabilities(String browserName, String version,
                String platform) {
            this.browserName = browserName;
            this.version = version;
            this.platform = platform;
        }

        public LoggingPrefs getLoggingPrefs() {
            return loggingPrefs;
        }

        public String getBrowserName() {
            return browserName;
        }

        public String getVersion() {
            return version;
        }

        public String getPlatform() {
            return platform;
        }

        public boolean isLive() {
            return live;
        }

        public Object getChromeOptions() {
            return chromeOptions;
        }

        public Object getOperaOptions() {
            return operaOptions;
        }

        public boolean isAcceptInsecureCerts() {
            return acceptInsecureCerts;
        }

        public String getFirefoxProfile() {
            return firefoxProfile;
        }

        public String getBrowserId() {
            return browserId;
        }

        public boolean isManualRecording() {
            return manualRecording;
        }

        public void setManualRecording(boolean manualRecording) {
            this.manualRecording = manualRecording;
        }

        @Override
        public String toString() {
            return "DesiredCapabilities [getLoggingPrefs()=" + getLoggingPrefs()
                    + ", getBrowserName()=" + getBrowserName()
                    + ", getVersion()=" + getVersion() + ", getPlatform()="
                    + getPlatform() + ", isLive()=" + isLive()
                    + ", getChromeOptions()=" + getChromeOptions()
                    + ", getOperaOptions()=" + getOperaOptions()
                    + ", isAcceptInsecureCerts()=" + isAcceptInsecureCerts()
                    + ", getFirefoxProfile()=" + getFirefoxProfile()
                    + ", getBrowserId()=" + getBrowserId()
                    + ", isManualRecording()=" + isManualRecording() + "]";
        }

    }

    public static class LoggingPrefs {
        String browser;

        public LoggingPrefs() {
            // Empty default construct
        }

        public String getBrowser() {
            return browser;
        }

        @Override
        public String toString() {
            return "LoggingPrefs [getBrowser()=" + getBrowser() + "]";
        }
    }

}
