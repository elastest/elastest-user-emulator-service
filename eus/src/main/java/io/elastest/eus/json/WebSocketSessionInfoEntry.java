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

import io.elastest.epm.client.model.DockerServiceStatus;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.session.SessionInfo;

/**
 * Utility JSON messages sent by WebSocket.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public class WebSocketSessionInfoEntry extends DockerServiceStatus {

    String id;
    String url;
    String browser;
    String version;
    String creationTime;
    String hubContainerName;
    String folderPath;
    boolean live;
    ExecutionData elastestExecutionData;

    public WebSocketSessionInfoEntry() {
        // Empty default construct (needed by Jackson)
    }

    public WebSocketSessionInfoEntry(SessionInfo sessionInfo) {
        this.id = sessionInfo.getIdForFiles();
        this.url = sessionInfo.getVncUrl();
        this.browser = sessionInfo.getBrowser();
        this.version = sessionInfo.getVersion();
        this.creationTime = sessionInfo.getCreationTime();
        this.hubContainerName = sessionInfo.getHubContainerName();
        this.folderPath = sessionInfo.getFolderPath();
        this.setStatus(sessionInfo.getStatus());
        this.setStatusMsg(sessionInfo.getStatusMsg());
        this.live = sessionInfo.isLiveSession();
        this.elastestExecutionData = sessionInfo.getElastestExecutionData();
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getBrowser() {
        return browser;
    }

    public String getVersion() {
        return version;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public String getHubContainerName() {
        return hubContainerName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public boolean isLive() {
        return live;
    }

    public ExecutionData getElastestExecutionData() {
        return elastestExecutionData;
    }

    @Override
    public String toString() {
        return "WebSocketSessionInfoEntry [id=" + id + ", url=" + url
                + ", browser=" + browser + ", version=" + version
                + ", creationTime=" + creationTime + ", hubContainerName="
                + hubContainerName + ", folderPath=" + folderPath + ", live="
                + live + ", elastestExecutionData=" + elastestExecutionData
                + ", toString()=" + super.toString() + "]";
    }

}
