package io.elastest.eus.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecutionData {

    @JsonProperty("type")
    String type;

    @JsonProperty("tJobId")
    Long tJobId;

    @JsonProperty("tJobExecId")
    Long tJobExecId;

    @JsonProperty("monitoringIndex")
    String monitoringIndex;

    @JsonProperty("webRtcStatsActivated")
    boolean webRtcStatsActivated;

    @JsonProperty("folderPath")
    String folderPath;

    public ExecutionData() {
    }

    public ExecutionData(String type, Long tJobId, Long tJobExecId,
            String monitoringIndex, boolean webRtcStatsActivated,
            String folderPath) {
        super();
        this.type = type;
        this.tJobId = tJobId;
        this.tJobExecId = tJobExecId;
        this.monitoringIndex = monitoringIndex;
        this.webRtcStatsActivated = webRtcStatsActivated;
        this.folderPath = folderPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long gettJobId() {
        return tJobId;
    }

    public void settJobId(Long tJobId) {
        this.tJobId = tJobId;
    }

    public Long gettJobExecId() {
        return tJobExecId;
    }

    public void settJobExecId(Long tJobExecId) {
        this.tJobExecId = tJobExecId;
    }

    public String getMonitoringIndex() {
        return monitoringIndex;
    }

    public void setMonitoringIndex(String monitoringIndex) {
        this.monitoringIndex = monitoringIndex;
    }

    public boolean isWebRtcStatsActivated() {
        return webRtcStatsActivated;
    }

    public void setWebRtcStatsActivated(boolean webRtcStatsActivated) {
        this.webRtcStatsActivated = webRtcStatsActivated;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    public String toString() {
        return "EusExecutionData [type=" + type + ", tJobId=" + tJobId
                + ", tJobExecId=" + tJobExecId + ", monitoringIndex="
                + monitoringIndex + ", webRtcStatsActivated="
                + webRtcStatsActivated + ", folderPath=" + folderPath + "]";
    }

    public String getKey() {
        return type + "_" + tJobId + "_" + tJobExecId;
    }
}
