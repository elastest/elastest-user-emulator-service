package io.elastest.eus.api.model;

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

/**
 * StatsValue
 */
public class StatsValue {
    private String id = null;

    private String stats = null;

    public StatsValue id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Peerconnection identifier
     * 
     * @return id
     **/
    @ApiModelProperty(value = "Peerconnection identifier")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StatsValue stats(String stats) {
        this.stats = stats;
        return this;
    }

    /**
     * All Peerconnection stats
     * 
     * @return stats
     **/
    @ApiModelProperty(value = "All Peerconnection stats")
    public String getStats() {
        return stats;
    }

    public void setStats(String stats) {
        this.stats = stats;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatsValue statsValue = (StatsValue) o;
        return Objects.equals(this.id, statsValue.id)
                && Objects.equals(this.stats, statsValue.stats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stats);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StatsValue {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    stats: ").append(toIndentedString(stats)).append("\n");
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
