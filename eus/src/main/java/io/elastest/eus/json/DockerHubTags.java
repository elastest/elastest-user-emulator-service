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
package io.elastest.eus.json;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Docker Hub tags.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.1
 */
public class DockerHubTags {

    int count;
    Object next;
    Object previous;
    List<DockerHubTag> results;

    public List<DockerHubTag> getResults() {
        return results;
    }

    public class DockerHubTag {
        String name;

        @SerializedName("full_size")
        String fullSize;

        List<Image> images;

        long id;
        long repository;
        long creator;

        @SerializedName("last_updater")
        long lastUpdater;

        @SerializedName("last_updated")
        String lastUpdated;

        @SerializedName("image_id")
        Object imageId;

        boolean v2;

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }

    }

    class Image {
        long size;
        String architecture;
        Object variant;
        Object features;
        String os;

        @SerializedName("os_version")
        Object osVersion;

        @SerializedName("os_features")
        Object osFeatures;
    }

}
