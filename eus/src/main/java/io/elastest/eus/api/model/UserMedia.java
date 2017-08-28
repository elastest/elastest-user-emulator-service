/*
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.elastest.eus.api.model;

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

/**
 * UserMedia.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class UserMedia {
    private String mediaUrl = null;
    private Boolean video = true;
    private Boolean audio = true;

    public UserMedia mediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    /**
     * URL of media (video, audio, or both) to fake WebRTC user media
     * 
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
     * 
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
     * 
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
        return Objects.equals(this.mediaUrl, userMedia.mediaUrl)
                && Objects.equals(this.video, userMedia.video)
                && Objects.equals(this.audio, userMedia.audio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaUrl, video, audio);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UserMedia {\n");

        sb.append("    mediaUrl: ").append(toIndentedString(mediaUrl))
                .append("\n");
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