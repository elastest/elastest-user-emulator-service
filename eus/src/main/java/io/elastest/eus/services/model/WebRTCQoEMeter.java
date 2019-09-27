package io.elastest.eus.services.model;

public class WebRTCQoEMeter extends EusServiceModel {
    boolean csvGenerated;

    public WebRTCQoEMeter() {
        super(EusServiceName.WEBRTC_QOE_METER);
        this.csvGenerated = false;
    }

    public boolean isCsvGenerated() {
        return csvGenerated;
    }

    public void setCsvGenerated(boolean csvGenerated) {
        this.csvGenerated = csvGenerated;
    }

}
