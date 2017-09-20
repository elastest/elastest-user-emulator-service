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
package io.elastest.eus.external;

import io.elastest.eus.json.EdmAluxioFile;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Alluxio REST service (provided by EDM) API description.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public interface EdmAluxioApi {

    @POST("/api/v1/paths//{file}/open-file")
    Call<ResponseBody> openFile(@Path("file") String file);

    @POST("/api/v1/streams/{streamId}/read")
    Call<ResponseBody> readStream(@Path("streamId") String streamId);

    @POST("/api/v1/paths//{file}/create-file")
    Call<ResponseBody> createFile(@Path("file") String file);

    @POST("/api/v1/streams/{streamId}/write")
    Call<Void> writeStream(@Path("streamId") String streamId,
            @Body RequestBody fileContent);

    @POST("/api/v1/streams/{streamId}/close")
    Call<ResponseBody> closeStream(@Path("streamId") String streamId);

    @POST("/api/v1/paths//{file}/delete")
    Call<ResponseBody> deleteFile(@Path("file") String file);

    @POST("/api/v1/paths//{folder}/list-status")
    Call<EdmAluxioFile[]> listFiles(@Path("folder") String folder);

}
