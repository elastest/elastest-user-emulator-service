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

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for recording capabilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Service
public class RecordingService {

    private final Logger log = LoggerFactory.getLogger(RecordingService.class);

    @Value("${registry.folder}")
    private String registryFolder;

    @Value("${registry.recording.extension}")
    private String registryRecordingExtension;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${registry.contextPath}")
    private String registryContextPath;

    @Value("${registry.metadata.extension}")
    private String registryMetadataExtension;

    @Value("${edm.alluxio.url}")
    private String edmAlluxioUrl;

    private DockerService dockerService;
    private JsonService jsonService;
    private AlluxioService alluxioService;

    @PostConstruct
    private void postConstruct() {
        // Ensure that EDM Alluxio URL (if available) ends with "/"
        if (!edmAlluxioUrl.isEmpty() && !edmAlluxioUrl.endsWith("/")) {
            edmAlluxioUrl += "/";
        }
    }

    @Autowired
    public RecordingService(DockerService dockerService,
            JsonService jsonService, AlluxioService alluxioService) {
        this.dockerService = dockerService;
        this.jsonService = jsonService;
        this.alluxioService = alluxioService;
    }

    public void startRecording(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String noNvcContainerName = sessionInfo.getVncContainerName();
        String hubContainerIp = dockerService.getDockerServerIp();
        String hubContainerPort = String
                .valueOf(sessionInfo.getHubVncBindPort());

        log.debug("Recording session {} in container {} ({}:{})", sessionId,
                noNvcContainerName, hubContainerIp, hubContainerPort);

        dockerService.execCommand(noNvcContainerName, false, "/novnc.sh",
                "--start", sessionId, hubContainerIp, hubContainerPort);
    }

    public void stopRecording(SessionInfo sessionInfo) {
        String noNvcContainerName = sessionInfo.getVncContainerName();
        log.trace("Stopping recording of container {}", noNvcContainerName);
        dockerService.execCommand(noNvcContainerName, false, "/novnc.sh",
                "--end");
    }

    public void storeRecording(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String noNvcContainerName = sessionInfo.getVncContainerName();
        String recordingFileName = sessionId + registryRecordingExtension;

        try {
            // Convert format of recording to mp4
            dockerService.execCommand(noNvcContainerName, true, "ffmpeg", "-i",
                    sessionId + ".flv", "-c:v", "libx264", "-crf", "19",
                    "-strict", "experimental", recordingFileName);

            if (edmAlluxioUrl.isEmpty()) {
                // If EDM Alluxio is not available, recording is stored locally
                String target = registryFolder + recordingFileName;

                InputStream inputStream = dockerService.getFileFromContainer(
                        noNvcContainerName, recordingFileName);

                // -------------
                // FIXME: Workaround due to strange behavior of docker-java
                // it seems that copyArchiveFromContainerCmd not works correctly

                byte[] bytes = IOUtils.toByteArray(inputStream);

                int i = 0;
                for (; i < bytes.length; i++) {
                    char c1 = (char) bytes[i];
                    if (c1 == 'f') {
                        char c2 = (char) bytes[i + 1];
                        char c3 = (char) bytes[i + 2];
                        if (c2 == 't' && c3 == 'y') {
                            break;
                        }
                    }
                }

                FileUtils.writeByteArrayToFile(new File(target),
                        Arrays.copyOfRange(bytes, i - 4, bytes.length));
                // -------------

            } else {
                // If EDM Alluxio is available, recording is stored in Alluxio
                dockerService.execCommand(noNvcContainerName, true, "/novnc.sh",
                        "--upload", edmAlluxioUrl, recordingFileName);
            }

        } catch (IOException e) {
            log.error("Exception storing recording (sessiodId {})",
                    sessionInfo.getSessionId(), e);
        }
    }

    public void storeMetadata(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String metadataFileName = sessionId + registryMetadataExtension;
        JSONObject sessionInfoToJson = jsonService
                .recordedSessionJson(sessionInfo);
        try {
            if (edmAlluxioUrl.isEmpty()) {
                // If EDM Alluxio is not available, metadata is stored locally

                FileUtils.writeStringToFile(
                        new File(registryFolder + metadataFileName),
                        sessionInfoToJson.toString(), Charset.defaultCharset());

            } else {
                // If EDM Alluxio is available, recording is stored in Alluxio
                alluxioService.writeFile(metadataFileName,
                        sessionInfoToJson.toString().getBytes());
            }

        } catch (IOException e) {
            log.error("Exception storing metadata (sessiodId {})",
                    sessionInfo.getSessionId(), e);
        }
    }

    public ResponseEntity<String> getRecording(String sessionId) {
        HttpStatus status = OK;
        String recordingFileName = sessionId + registryRecordingExtension;

        // By default the response is the local path for the recording (this
        // applies to the case of locally stored, and also to the case that the
        // recording has been previously downloaded from Alluxio)
        String urlResponse = contextPath + registryContextPath + "/"
                + recordingFileName;

        if (!edmAlluxioUrl.isEmpty()) {
            // If EDM Alluxio is available, recording is store in Alluxio
            File targetFile = new File(registryFolder + recordingFileName);
            if (!targetFile.exists()) {
                byte[] file;
                try {
                    file = alluxioService.getFile(recordingFileName);
                    FileUtils.writeByteArrayToFile(targetFile, file);
                } catch (IOException e) {
                    status = INTERNAL_SERVER_ERROR;
                    log.error(
                            "Error getting recording of session {} from Alluxio",
                            sessionId, e);
                }
            }
        }

        ResponseEntity<String> responseEntity = new ResponseEntity<>(
                urlResponse, status);
        return responseEntity;
    }

    public ResponseEntity<String> deleteRecording(String sessionId) {
        log.debug("Deleting recording of session {}", sessionId);
        String recordingFileName = sessionId + registryRecordingExtension;
        String metadataFileName = sessionId + registryMetadataExtension;

        HttpStatus status = OK;
        if (edmAlluxioUrl.isEmpty()) {
            // If EDM Alluxio is not available, delete is done locally
            boolean deleteRecording = new File(
                    registryFolder + recordingFileName).delete();
            boolean deleteMetadata = new File(registryFolder + metadataFileName)
                    .delete();
            status = deleteRecording && deleteMetadata ? OK
                    : INTERNAL_SERVER_ERROR;

        } else {
            try {
                // If EDM Alluxio is available, deleting is done in Alluxio
                alluxioService.deleteFile(recordingFileName);
                alluxioService.deleteFile(metadataFileName);

            } catch (IOException e) {
                status = INTERNAL_SERVER_ERROR;
                log.error("Error deleting session {} from Alluxio", sessionId,
                        e);
            }

        }

        ResponseEntity<String> responseEntity = new ResponseEntity<>(status);
        log.debug("... response {}", status);
        return responseEntity;
    }

    public List<String> getStoredMetadataContent() {
        List<String> metadataContent = new ArrayList<>();

        if (edmAlluxioUrl.isEmpty()) {
            // If EDM Alluxio is not available, recordings and metadata are
            // stored locally
            File[] metadataFiles = new File(registryFolder)
                    .listFiles((dir, name) -> {
                        return name.toLowerCase()
                                .endsWith(registryMetadataExtension);
                    });
            metadataContent = stream(metadataFiles).map(f -> {
                String content = "";
                try {
                    content = new String(
                            readAllBytes(get(f.getAbsolutePath())));
                } catch (IOException e) {
                    log.error("Exception reading content of local metadata {}",
                            f, e);
                }
                return content;
            }).collect(toList());

            new File(registryFolder).listFiles();

        } else {
            // If EDM Alluxio is available, recordings and metadata are stored
            // in Alluxio
            List<String> metadataFileList;
            try {
                metadataFileList = alluxioService.getMetadataFileList();
                metadataContent = metadataFileList.stream().map(f -> {
                    String content = "";
                    try {
                        content = new String(alluxioService.getFile(f));
                    } catch (IOException e) {
                        log.error(
                                "Exception reading content of Alluxio metadata {}",
                                f, e);
                    }
                    return content;
                }).collect(toList());

            } catch (IOException e) {
                log.error(
                        "Exception reading content of metadata file from Alluxio",
                        e);
            }

        }

        return metadataContent;
    }

}
