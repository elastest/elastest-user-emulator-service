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

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Docker Hub API.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.1
 */
public interface DockerHubApi {

    @GET("/v2/repositories/{namespace}/?page_size=1024")
    Call<DockerHubNameSpaceImages> listImages(
            @Path("namespace") String namespace);

    @GET("/v2/repositories/{imageName}/tags/?page_size=1024")
    Call<DockerHubTags> listTags(@Path("imageName") String imageName);

    @GET("/v2/repositories/selenoid/vnc/tags/?page_size=1024")
    Call<DockerHubTags> listSelenoidVncTags();
}
