package io.elastest.eus.api.model;

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

/**
 * Quality
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2017-06-01T16:29:27.571+02:00")

public class Quality {
    private String senderSessionId = null;

    private String senderElementId = null;

    /**
     * QoE algorithm (full-reference) to be used
     */
    public enum AlgorithmEnum {
        PESQ("pesq"),

        SSIM("ssim"),

        PSNR("psnr");

        private String value;

        AlgorithmEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private AlgorithmEnum algorithm = null;

    private Integer sampleRate = 1000;

    public Quality senderSessionId(String senderSessionId) {
        this.senderSessionId = senderSessionId;
        return this;
    }

    /**
     * Session identifier of the WebRTC media peer producer
     * 
     * @return senderSessionId
     **/
    @ApiModelProperty(value = "Session identifier of the WebRTC media peer producer")
    public String getSenderSessionId() {
        return senderSessionId;
    }

    public void setSenderSessionId(String senderSessionId) {
        this.senderSessionId = senderSessionId;
    }

    public Quality senderElementId(String senderElementId) {
        this.senderElementId = senderElementId;
        return this;
    }

    /**
     * Element identifier of the video tag producer
     * 
     * @return senderElementId
     **/
    @ApiModelProperty(value = "Element identifier of the video tag producer")
    public String getSenderElementId() {
        return senderElementId;
    }

    public void setSenderElementId(String senderElementId) {
        this.senderElementId = senderElementId;
    }

    public Quality algorithm(AlgorithmEnum algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    /**
     * QoE algorithm (full-reference) to be used
     * 
     * @return algorithm
     **/
    @ApiModelProperty(value = "QoE algorithm (full-reference) to be used")
    public AlgorithmEnum getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmEnum algorithm) {
        this.algorithm = algorithm;
    }

    public Quality sampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    /**
     * Sample rate for the quality evaluation (in milliseconds)
     * 
     * @return sampleRate
     **/
    @ApiModelProperty(value = "Sample rate for the quality evaluation (in milliseconds)")
    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Quality quality = (Quality) o;
        return Objects.equals(this.senderSessionId, quality.senderSessionId)
                && Objects.equals(this.senderElementId, quality.senderElementId)
                && Objects.equals(this.algorithm, quality.algorithm)
                && Objects.equals(this.sampleRate, quality.sampleRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderSessionId, senderElementId, algorithm,
                sampleRate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Quality {\n");

        sb.append("    senderSessionId: ")
                .append(toIndentedString(senderSessionId)).append("\n");
        sb.append("    senderElementId: ")
                .append(toIndentedString(senderElementId)).append("\n");
        sb.append("    algorithm: ").append(toIndentedString(algorithm))
                .append("\n");
        sb.append("    sampleRate: ").append(toIndentedString(sampleRate))
                .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
