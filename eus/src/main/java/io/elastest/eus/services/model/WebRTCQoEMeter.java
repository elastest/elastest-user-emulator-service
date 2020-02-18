package io.elastest.eus.services.model;

import java.util.HashMap;
import java.util.Map;

import io.elastest.eus.json.VideoTimeInfo;

public class WebRTCQoEMeter extends EusServiceModel {
    boolean csvGenerated;
    boolean errorOnCsvGeneration;
    Map<String, byte[]> csvs;
    VideoTimeInfo videoTimeInfo;
    long numberOfFrames;

    public WebRTCQoEMeter() {
        super(EusServiceName.WEBRTC_QOE_METER);
        this.csvGenerated = false;
        this.errorOnCsvGeneration = false;
        this.csvs = new HashMap<String, byte[]>();
    }

    public boolean isCsvGenerated() {
        return csvGenerated;
    }

    public void setCsvGenerated(boolean csvGenerated) {
        this.csvGenerated = csvGenerated;
    }

    public boolean isErrorOnCsvGeneration() {
        return errorOnCsvGeneration;
    }

    public void setErrorOnCsvGeneration(boolean errorOnCsvGeneration) {
        this.errorOnCsvGeneration = errorOnCsvGeneration;
    }

    public Map<String, byte[]> getCsvs() {
        return csvs;
    }

    public void setCsvs(Map<String, byte[]> csvs) {
        this.csvs = csvs;
    }

    public VideoTimeInfo getVideoTimeInfo() {
        return videoTimeInfo;
    }

    public void setVideoTimeInfo(VideoTimeInfo videoTimeInfo) {
        this.videoTimeInfo = videoTimeInfo;
    }

    public long getNumberOfFrames() {
        return numberOfFrames;
    }

    public void setNumberOfFrames(long numberOfFrames) {
        this.numberOfFrames = numberOfFrames;
    }

    @Override
    public String toString() {
        return "WebRTCQoEMeter [csvGenerated=" + csvGenerated + ", errorOnCsvGeneration="
                + errorOnCsvGeneration + ", csvs=" + csvs + "]";
    }

}
