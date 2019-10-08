package io.elastest.eus.services.model;

public class WebRTCQoEMeter extends EusServiceModel {
    boolean csvGenerated;
    boolean errorOnCsvGeneration;

    public WebRTCQoEMeter() {
        super(EusServiceName.WEBRTC_QOE_METER);
        this.csvGenerated = false;
        this.errorOnCsvGeneration = false;
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

    @Override
    public String toString() {
        return "WebRTCQoEMeter [csvGenerated=" + csvGenerated
                + ", errorOnCsvGeneration=" + errorOnCsvGeneration + "]";
    }

}
