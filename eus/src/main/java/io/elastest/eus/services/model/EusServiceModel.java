package io.elastest.eus.services.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class EusServiceModel {
    protected EusServiceName serviceName;
    // Usually container name
    protected String identifier;

    private String awsInstanceId;

    public EusServiceModel(EusServiceName serviceName) {
        this.serviceName = serviceName;
    }

    /* ******************************* */
    /* ******* Getters/Setters ******* */
    /* ******************************* */

    public EusServiceName getServiceName() {
        return serviceName;
    }

    protected void setServiceName(EusServiceName serviceName) {
        this.serviceName = serviceName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getAwsInstanceId() {
        return awsInstanceId;
    }

    public void setAwsInstanceId(String awsInstanceId) {
        this.awsInstanceId = awsInstanceId;
    }

    /* ***************************** */
    /* ******* Other methods ******* */
    /* ***************************** */

    @Override
    public String toString() {
        return "EusServiceModel [serviceName=" + serviceName + ", identifier="
                + identifier + ", awsInstanceId=" + awsInstanceId + "]";
    }

    /* ***************************** */
    /* *********** Enums *********** */
    /* ***************************** */
    public enum EusServiceName {
        BROWSERSYNC("browsersync"), WEBRTC_QOE_METER("webrtc_qoe_meter");

        private String value;

        EusServiceName(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static EusServiceName fromValue(String text) {
            for (EusServiceName b : EusServiceName.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

}
