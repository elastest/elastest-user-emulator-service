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
package io.elastest.eus.session;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.platform.manager.DockerBrowserInfo;
import io.elastest.eus.platform.manager.PlatformManager;

/**
 * Session information.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class SessionManager extends DockerBrowserInfo {
    final Logger log = getLogger(lookup().lookupClass());
    private static String HUB_PATH = "/wd/hub";

    private String sessionId;
    private String hubUrl;
    private String creationTime;
    private String browser;
    private String version;
    private boolean liveSession;
    private List<Future<?>> timeoutFutures = new CopyOnWriteArrayList<>();
    private int timeout;
    private String testName;
    private boolean manualRecording;
    private String folderPath;
    ExecutionData elastestExecutionData;
    DesiredCapabilities capabilities;

    private String awsInstanceId;

    @JsonIgnore
    PlatformManager platformManager;

    public SessionManager(PlatformManager platformManager) {
        super();
        this.platformManager = platformManager;
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isLiveSession() {
        return liveSession;
    }

    public void setLiveSession(boolean liveSession) {
        this.liveSession = liveSession;
    }

    public List<Future<?>> getTimeoutFutures() {
        return timeoutFutures;
    }

    public void addTimeoutFuture(Future<?> timeoutFuture) {
        this.timeoutFutures.add(timeoutFuture);
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public boolean isManualRecording() {
        return manualRecording;
    }

    public void setManualRecording(boolean manualRecording) {
        this.manualRecording = manualRecording;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public ExecutionData getElastestExecutionData() {
        return elastestExecutionData;
    }

    public void setElastestExecutionData(ExecutionData elastestExecutionData) {
        this.elastestExecutionData = elastestExecutionData;
    }

    public DesiredCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(DesiredCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public PlatformManager getPlatformManager() {
        return platformManager;
    }

    public void setPlatformManager(PlatformManager platformManager) {
        this.platformManager = platformManager;
    }

    public String getAwsInstanceId() {
        return awsInstanceId;
    }

    public void setAwsInstanceId(String awsInstanceId) {
        this.awsInstanceId = awsInstanceId;
    }
    
    /* ********************************************* */
    /* *************** Other methods *************** */
    /* ********************************************* */

    public boolean isAWSSession() {
        return this.capabilities != null
                && this.capabilities.getAwsConfig() != null;
    }

    public String getIdForFiles() {
        String id = sessionId;
        if (testName != null && !testName.isEmpty()) {
            id = testName;
            id = id.replaceAll(" ", "-");
            id = id + "_" + sessionId;
        }
        return id;
    }

    public void buildHubUrl() {
        hubUrl = "http://" + hubIp + ":" + hubPort + HUB_PATH;
    }

    @Override
    public String toString() {
        return "SessionInfo [log=" + log + ", sessionId=" + sessionId
                + ", hubUrl=" + hubUrl + ", creationTime=" + creationTime
                + ", browser=" + browser + ", version=" + version
                + ", liveSession=" + liveSession + ", timeoutFutures="
                + timeoutFutures + ", timeout=" + timeout + ", testName="
                + testName + ", manualRecording=" + manualRecording
                + ", folderPath=" + folderPath + ", elastestExecutionData="
                + elastestExecutionData + ", capabilities=" + capabilities
                + ", toString()=" + super.toString() + "]";
    }

    public String getBrowserServiceNameOrId() {
        return isAWSSession() ? this.awsInstanceId : this.getVncContainerName();
    }
}
