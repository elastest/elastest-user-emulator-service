package io.elastest.eus.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecutionData {

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

    public ExecutionData(Long tJobId, Long tJobExecId, String monitoringIndex,
            boolean webRtcStatsActivated, String folderPath) {
        super();
        this.tJobId = tJobId;
        this.tJobExecId = tJobExecId;
        this.monitoringIndex = monitoringIndex;
        this.webRtcStatsActivated = webRtcStatsActivated;
        this.folderPath = folderPath;
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
        return "ExecutionData [tJobId=" + tJobId + ", tJobExecId=" + tJobExecId
                + ", monitoringIndex=" + monitoringIndex
                + ", webRtcStatsActivated=" + webRtcStatsActivated
                + ", folderPath=" + folderPath + "]";
    }

    public String getKey() {
        return tJobId + "_" + tJobExecId;
    }
}
