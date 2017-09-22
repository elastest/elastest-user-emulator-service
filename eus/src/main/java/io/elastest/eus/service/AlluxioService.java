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

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.elastest.eus.EusException;
import io.elastest.eus.external.EdmAluxioApi;
import io.elastest.eus.json.EdmAluxioFile;
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

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${edm.alluxio.url}")
    private String edmAlluxioUrl;

    @Value("${registry.metadata.extension}")
    private String metadataExtension;

    private EdmAluxioApi alluxio;

    @PostConstruct
    public void postConstruct() {
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

    public String getFileAsString(String file) {
        try {
            return new String(getFile(file));
        } catch (IOException e) {
            String errorMessage = "Exception getting file from Alluxio: "
                    + file;
            // Not propagating IOException to improve readability
            throw new EusException(errorMessage, e);
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

    public boolean writeFile(String fileName, byte[] fileContent)
            throws IOException {
        log.debug("Writing {} bytes to Alluxio", fileContent.length);

        Call<ResponseBody> openFile = alluxio.createFile(fileName);
        String streamId = openFile.execute().body().string();
        log.debug("Stream id {}", streamId);

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/octet-stream"), fileContent);
        Response<Void> execute = alluxio.writeStream(streamId, requestBody)
                .execute();
        log.debug("Result: {}", execute);

        boolean writeSuccessful = execute.isSuccessful();

        alluxio.closeStream(streamId).execute();
        log.debug("Stream {} closed", streamId);

        return writeSuccessful;
    }

    public boolean deleteFile(String file) throws IOException {
        log.debug("Deleting file {}", file);
        Response<ResponseBody> response = alluxio.deleteFile(file).execute();
        log.debug("Reponse: {}", response);
        return response.isSuccessful();
    }

    public List<String> listFiles(String folder) throws IOException {
        log.trace("Listing Alluxio files in folder {}", folder);
        EdmAluxioFile[] files = alluxio.listFiles(folder).execute().body();
        if (log.isDebugEnabled()) {
            log.debug("List files response: {}", Arrays.toString(files));
        }
        return stream(files).map(EdmAluxioFile::getName).collect(toList());
    }

    public List<String> getMetadataFileList() throws IOException {
        List<String> listFiles = listFiles("/");
        return listFiles.stream().filter(f -> f.endsWith(metadataExtension))
                .collect(Collectors.toList());
    }

}
