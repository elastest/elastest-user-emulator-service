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

import io.swagger.annotations.ApiModelProperty;

/**
 * StatsValue.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
public class StatsValue {
    private String id = null;
    private String stats = null;

    public StatsValue id(String id) {
        this.id = id;
        return this;
    }

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

    @ApiModelProperty(value = "All Peerconnection stats")
    public String getStats() {
        return stats;
    }

    public void setStats(String stats) {
        this.stats = stats;
    }

}
