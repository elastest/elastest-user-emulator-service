package io.elastest.eus.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;




/**
 * Latency
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2017-06-01T16:29:27.571+02:00")

public class Latency   {
  private String senderSessionId = null;

  private String senderElementId = null;

  private Integer sampleRate = 1000;

  public Latency senderSessionId(String senderSessionId) {
    this.senderSessionId = senderSessionId;
    return this;
  }

   /**
   * Session identifier of the WebRTC media peer producer
   * @return senderSessionId
  **/
  @ApiModelProperty(value = "Session identifier of the WebRTC media peer producer")
  public String getSenderSessionId() {
    return senderSessionId;
  }

  public void setSenderSessionId(String senderSessionId) {
    this.senderSessionId = senderSessionId;
  }

  public Latency senderElementId(String senderElementId) {
    this.senderElementId = senderElementId;
    return this;
  }

   /**
   * Element identifier of the video tag producer
   * @return senderElementId
  **/
  @ApiModelProperty(value = "Element identifier of the video tag producer")
  public String getSenderElementId() {
    return senderElementId;
  }

  public void setSenderElementId(String senderElementId) {
    this.senderElementId = senderElementId;
  }

  public Latency sampleRate(Integer sampleRate) {
    this.sampleRate = sampleRate;
    return this;
  }

   /**
   * Sample rate for the quality evaluation (in milliseconds)
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
    Latency latency = (Latency) o;
    return Objects.equals(this.senderSessionId, latency.senderSessionId) &&
        Objects.equals(this.senderElementId, latency.senderElementId) &&
        Objects.equals(this.sampleRate, latency.sampleRate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(senderSessionId, senderElementId, sampleRate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Latency {\n");
    
    sb.append("    senderSessionId: ").append(toIndentedString(senderSessionId)).append("\n");
    sb.append("    senderElementId: ").append(toIndentedString(senderElementId)).append("\n");
    sb.append("    sampleRate: ").append(toIndentedString(sampleRate)).append("\n");
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

