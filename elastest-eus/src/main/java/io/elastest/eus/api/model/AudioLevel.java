package io.elastest.eus.api.model;

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

/**
 * AudioLevel
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2017-06-01T16:29:27.571+02:00")

public class AudioLevel {
    private String audio = null;

    public AudioLevel audio(String audio) {
        this.audio = audio;
        return this;
    }

    /**
     * Audio level
     * 
     * @return audio
     **/
    @ApiModelProperty(value = "Audio level")
    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
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
        AudioLevel audioLevel = (AudioLevel) o;
        return Objects.equals(this.audio, audioLevel.audio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(audio);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AudioLevel {\n");

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
