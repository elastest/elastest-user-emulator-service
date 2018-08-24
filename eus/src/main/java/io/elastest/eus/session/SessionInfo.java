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

import io.elastest.epm.client.model.DockerServiceStatus;

/**
 * Session information.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class SessionInfo extends DockerServiceStatus {
    final Logger log = getLogger(lookup().lookupClass());

    private String sessionId;
    private String hubUrl;
    private String hubContainerName;
    private String vncUrl;
    private String vncContainerName;
    private String creationTime;
    private String browser;
    private String version;
    private boolean liveSession;
    private List<Future<?>> timeoutFutures = new CopyOnWriteArrayList<>();
    private int hubBindPort;
    private int hubVncBindPort;
    private int noVncBindPort;
    private int timeout;
    private String browserId;
    private boolean manualRecording;
    private String folderPath;

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public String getHubContainerName() {
        return hubContainerName;
    }

    public void setHubContainerName(String hubContainerName) {
        this.hubContainerName = hubContainerName;
    }

    public String getVncUrl() {
        return vncUrl;
    }

    public void setVncUrl(String vncUrl) {
        this.vncUrl = vncUrl;
    }

    public String getVncContainerName() {
        return vncContainerName;
    }

    public void setVncContainerName(String vncContainerName) {
        this.vncContainerName = vncContainerName;
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

    public int getHubBindPort() {
        return hubBindPort;
    }

    public void setHubBindPort(int hubBindPort) {
        this.hubBindPort = hubBindPort;
    }

    public int getHubVncBindPort() {
        return hubVncBindPort;
    }

    public void setHubVncBindPort(int hubVncBindPort) {
        this.hubVncBindPort = hubVncBindPort;
    }

    public int getNoVncBindPort() {
        return noVncBindPort;
    }

    public void setNoVncBindPort(int noVncBindPort) {
        this.noVncBindPort = noVncBindPort;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getBrowserId() {
        return browserId;
    }

    public void setBrowserId(String browserId) {
        this.browserId = browserId;
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

    public String getIdForFiles() {
        return browserId != null && !browserId.isEmpty()
                ? browserId + "_" + sessionId
                : sessionId;
    }

}
