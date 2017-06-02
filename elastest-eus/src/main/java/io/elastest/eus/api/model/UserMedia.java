package io.elastest.eus.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;




/**
 * UserMedia
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2017-06-01T16:29:27.571+02:00")

public class UserMedia   {
  private String mediaUrl = null;

  private Boolean video = true;

  private Boolean audio = true;

  public UserMedia mediaUrl(String mediaUrl) {
    this.mediaUrl = mediaUrl;
    return this;
  }

   /**
   * URL of media (video, audio, or both) to fake WebRTC user media
   * @return mediaUrl
  **/
  @ApiModelProperty(value = "URL of media (video, audio, or both) to fake WebRTC user media")
  public String getMediaUrl() {
    return mediaUrl;
  }

  public void setMediaUrl(String mediaUrl) {
    this.mediaUrl = mediaUrl;
  }

  public UserMedia video(Boolean video) {
    this.video = video;
    return this;
  }

   /**
   * Flag that indicates whether or not the video should be faked
   * @return video
  **/
  @ApiModelProperty(value = "Flag that indicates whether or not the video should be faked")
  public Boolean getVideo() {
    return video;
  }

  public void setVideo(Boolean video) {
    this.video = video;
  }

  public UserMedia audio(Boolean audio) {
    this.audio = audio;
    return this;
  }

   /**
   * Flag that indicates whether or not the audio should be faked
   * @return audio
  **/
  @ApiModelProperty(value = "Flag that indicates whether or not the audio should be faked")
  public Boolean getAudio() {
    return audio;
  }

  public void setAudio(Boolean audio) {
    this.audio = audio;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserMedia userMedia = (UserMedia) o;
    return Objects.equals(this.mediaUrl, userMedia.mediaUrl) &&
        Objects.equals(this.video, userMedia.video) &&
        Objects.equals(this.audio, userMedia.audio);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mediaUrl, video, audio);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserMedia {\n");
    
    sb.append("    mediaUrl: ").append(toIndentedString(mediaUrl)).append("\n");
    sb.append("    video: ").append(toIndentedString(video)).append("\n");
    sb.append("    audio: ").append(toIndentedString(audio)).append("\n");
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

