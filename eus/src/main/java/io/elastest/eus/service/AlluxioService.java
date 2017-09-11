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
package io.elastest.eus.service;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import io.elastest.eus.edm.EdmAluxioApi;
import io.elastest.eus.edm.EdmAluxioFile;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Alluxio Service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Service
public class AlluxioService {

    private final Logger log = LoggerFactory.getLogger(AlluxioService.class);

    @Value("${edm.alluxio.url}")
    private String edmAlluxioUrl;

    @Value("${registry.metadata.extension}")
    private String metadataExtension;

    private EdmAluxioApi alluxio;

    @PostConstruct
    private void postConstruct() {
        if (!edmAlluxioUrl.isEmpty()) {
            // Ensure that EDM Alluxio URL (if available) ends with "/"
            if (!edmAlluxioUrl.endsWith("/")) {
                edmAlluxioUrl += "/";
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(edmAlluxioUrl).build();
            alluxio = retrofit.create(EdmAluxioApi.class);
        }
    }

    public byte[] getFile(String file) throws IOException {
        Call<ResponseBody> openFile = alluxio.openFile(file);
        Response<ResponseBody> execute = openFile.execute();
        String streamId = execute.body().string();
        log.debug("Stream id {}", streamId);

        Call<ResponseBody> readStream = alluxio.readStream(streamId);
        byte[] content = readStream.execute().body().bytes();
        log.debug("Received {} bytes", content.length);

        alluxio.closeStream(streamId).execute();
        log.debug("Stream {} closed", streamId);

        return content;
    }

    public void writeFile(String fileName, byte[] fileContent)
            throws IOException {
        log.trace("Writing file content to Alluxio: {}",
                new String(fileContent));

        Call<ResponseBody> openFile = alluxio.createFile(fileName);
        String streamId = openFile.execute().body().string();
        log.debug("Stream id {}", streamId);

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/octet-stream"), fileContent);
        Response<Void> execute = alluxio.writeStream(streamId, requestBody)
                .execute();
        log.debug("Result: {}", execute);

        // Map<String, RequestBody> map = new HashMap<>();
        // RequestBody requestBody = RequestBody.create(
        // MediaType.parse("application/octet-stream"), fileContent);
        // map.put("data-binary", requestBody);
        // Response<Void> execute = alluxio.writeStream(streamId,
        // map).execute();
        // log.debug("Result: {}", execute);

        alluxio.closeStream(streamId).execute();
        log.debug("Stream {} closed", streamId);
    }

    public void deleteFile(String file) throws IOException {
        log.debug("Deleting file {}", file);
        Response<ResponseBody> response = alluxio.deleteFile(file).execute();
        log.debug("Reponse: {}", response);
    }

    public List<String> listFiles(String folder) throws IOException {
        Response<ResponseBody> execute = alluxio.listFiles(folder).execute();
        String responseBody = execute.body().string();
        log.trace("Listing Alluxio files in folder {}", folder);

        EdmAluxioFile[] files = new Gson().fromJson(responseBody,
                EdmAluxioFile[].class);
        return stream(files).map(f -> f.name).collect(toList());
    }

    public List<String> getMetadataFileList() throws IOException {
        List<String> listFiles = listFiles("/");
        return listFiles.stream().filter(f -> f.endsWith(metadataExtension))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        AlluxioService alluxioService = new AlluxioService();
        alluxioService.edmAlluxioUrl = "http://172.18.0.12:39999/";
        alluxioService.metadataExtension = ".eus";
        alluxioService.postConstruct();

        // Path path = Paths.get("/tmp/spring.log");
        // byte[] fileContent = Files.readAllBytes(path);
        // alluxioService.writeFile("spring.log", fileContent);

        // byte[] file = alluxioService
        // .getFile("630e91d0-b91d-42b1-89ea-4c5d0d87e75f.eus");
        // System.out.println(file.length);

        List<String> listFiles = alluxioService.getMetadataFileList();
        for (String f : listFiles) {
            System.out.println(f);
        }

    }

}
