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

    @JsonProperty("useSutNetwork")
    boolean useSutNetwork = false;

    @JsonProperty("sutContainerPrefix")
    String sutContainerPrefix;

    public ExecutionData() {
    }

    public ExecutionData(String type, Long tJobId, Long tJobExecId,
            String monitoringIndex, boolean webRtcStatsActivated,
            String folderPath, boolean useSutNetwork,
            String sutContainerPrefix) {
        super();
        this.type = type;
        this.tJobId = tJobId;
        this.tJobExecId = tJobExecId;
        this.monitoringIndex = monitoringIndex;
        this.webRtcStatsActivated = webRtcStatsActivated;
        this.folderPath = folderPath;
        this.useSutNetwork = useSutNetwork;
        this.sutContainerPrefix = sutContainerPrefix;
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

    public boolean isUseSutNetwork() {
        return useSutNetwork;
    }

    public void setUseSutNetwork(boolean useSutNetwork) {
        this.useSutNetwork = useSutNetwork;
    }

    public String getSutContainerPrefix() {
        return sutContainerPrefix;
    }

    public void setSutContainerPrefix(String sutContainerPrefix) {
        this.sutContainerPrefix = sutContainerPrefix;
    }

    @Override
    public String toString() {
        return "ExecutionData [type=" + type + ", tJobId=" + tJobId
                + ", tJobExecId=" + tJobExecId + ", monitoringIndex="
                + monitoringIndex + ", webRtcStatsActivated="
                + webRtcStatsActivated + ", folderPath=" + folderPath
                + ", useSutNetwork=" + useSutNetwork + ", sutContainerPrefix="
                + sutContainerPrefix + "]";
    }

    public String getKey() {
        return type + "_" + tJobId + "_" + tJobExecId;
    }
}
