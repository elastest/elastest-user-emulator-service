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
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.elastest.epm.client.service.DockerService;
import io.elastest.eus.EusException;
import io.elastest.eus.json.WebSocketRecordedSession;
import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for recording capabilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Service
public class RecordingService {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${et.files.path}")
    private String etFilesPath;

    @Value("${registry.recording.extension}")
    private String registryRecordingExtension;

    @Value("${api.context.path}")
    private String apiContextPath;

    @Value("${registry.contextPath}")
    private String registryContextPath;

    @Value("${registry.metadata.extension}")
    private String registryMetadataExtension;

    @Value("${edm.alluxio.url}")
    private String edmAlluxioUrl;

    @Value("${start.recording.script.filename}")
    private String startRecordingScript;

    @Value("${stop.recording.script.filename}")
    private String stopRecordingScript;

    @Value("${container.recording.folder}")
    private String containerRecordingFolder;

    private DockerService dockerService;
    private EusJsonService jsonService;
    private AlluxioService alluxioService;

    @PostConstruct
    private void postConstruct() {
        // Ensure several attributes ends with "/"
        if (!edmAlluxioUrl.isEmpty() && !edmAlluxioUrl.endsWith("/")) {
            edmAlluxioUrl += "/";
        }
        if (!etFilesPath.isEmpty() && !etFilesPath.endsWith("/")) {
            etFilesPath += "/";
        }
    }

    @Autowired
    public RecordingService(DockerService dockerService,
            EusJsonService jsonService, AlluxioService alluxioService) {
        this.dockerService = dockerService;
        this.jsonService = jsonService;
        this.alluxioService = alluxioService;
    }

    public void startRecording(String sessionId, String hubContainerName,
            String recordingFileName) throws Exception {
        log.debug("Recording session {} in container {} with file name {}",
                sessionId, hubContainerName, recordingFileName);

        dockerService.execCommand(hubContainerName, false, startRecordingScript,
                "-n", recordingFileName);
    }

    public void startRecording(SessionInfo sessionInfo) throws Exception {
        String sessionId = sessionInfo.getSessionId();
        String noVncContainerName = sessionInfo.getVncContainerName();
        String recordingFileName = sessionInfo.getIdForFiles();

        this.startRecording(sessionId, noVncContainerName, recordingFileName);
    }

    public void stopRecording(SessionInfo sessionInfo) throws Exception {
        String noNvcContainerName = sessionInfo.getVncContainerName();
        this.stopRecording(noNvcContainerName);
    }

    public void stopRecording(String hubContainerName) throws Exception {
        log.debug("Stopping recording of container {}", hubContainerName);
        dockerService.execCommand(hubContainerName, true, stopRecordingScript);
    }

    public void storeMetadata(SessionInfo sessionInfo) throws IOException {
        String idForFiles = sessionInfo.getIdForFiles();
        String metadataFileName = idForFiles + registryMetadataExtension;
        WebSocketRecordedSession recordedSession = new WebSocketRecordedSession(
                sessionInfo);
        log.debug("Storing metadata {}", recordedSession);
        String sessionInfoToJson = jsonService.objectToJson(recordedSession);
        String folderPath = sessionInfo.getFolderPath() != null
                ? sessionInfo.getFolderPath()
                : etFilesPath;
        log.debug("Storing metadata file in {}", folderPath);

        if (edmAlluxioUrl.isEmpty()) {
            // If EDM Alluxio is not available, metadata is stored locally

            File dir = new File(folderPath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("The " + folderPath
                            + " directory could not be created");
                }
            }

            FileUtils.writeStringToFile(new File(folderPath + File.separator + metadataFileName),
                    sessionInfoToJson, defaultCharset());

        } else {
            // If EDM Alluxio is available, recording is stored in Alluxio
            alluxioService.writeFile(metadataFileName,
                    sessionInfoToJson.getBytes());
        }
    }

    public ResponseEntity<String> getRecording(String sessionId)
            throws IOException {
        return this.getRecording(sessionId, etFilesPath);
    }

    public ResponseEntity<String> getRecording(String sessionId,
            String folderPath) throws IOException {
        HttpStatus status = OK;
        String recordingFileName = sessionId + registryRecordingExtension;

        // By default the response is the local path for the recording (this
        // applies to the case of locally stored, and also to the case that the
        // recording has been previously downloaded from Alluxio)
        String urlResponse = apiContextPath + registryContextPath + "/"
                + recordingFileName;

        if (!edmAlluxioUrl.isEmpty()) {
            // If EDM Alluxio is available, recording is store in Alluxio
            File targetFile = new File(folderPath + recordingFileName);
            if (!targetFile.exists()) {
                byte[] file = alluxioService.getFile(recordingFileName);
                writeByteArrayToFile(targetFile, file);
            }
        }

        return new ResponseEntity<>(urlResponse, status);
    }

    public ResponseEntity<String> deleteRecording(String sessionId)
            throws IOException {
        return this.deleteRecording(sessionId, etFilesPath);
    }

    public ResponseEntity<String> deleteRecording(String sessionId,
            String folderPath) throws IOException {
        log.debug("Deleting recording of session {}", sessionId);
        String recordingFileName = sessionId + registryRecordingExtension;
        String metadataFileName = sessionId + registryMetadataExtension;

        boolean deleteRecording;
        boolean deleteMetadata;
        if (edmAlluxioUrl.isEmpty()) {
            // If EDM Alluxio is not available, delete is done locally
            deleteRecording = Files
                    .deleteIfExists(Paths.get(folderPath + recordingFileName));
            deleteMetadata = Files
                    .deleteIfExists(Paths.get(folderPath + metadataFileName));

        } else {
            // If EDM Alluxio is available, deleting is done in Alluxio
            deleteRecording = alluxioService.deleteFile(recordingFileName);
            deleteMetadata = alluxioService.deleteFile(metadataFileName);

        }
        HttpStatus status = deleteRecording && deleteMetadata ? OK
                : INTERNAL_SERVER_ERROR;
        log.debug("... response {}", status);
        return new ResponseEntity<>(status);
    }

    public List<String> getStoredMetadataContent() throws IOException {
        List<String> metadataContent = new ArrayList<>();

        if (edmAlluxioUrl.isEmpty()) {
            // If EDM Alluxio is not available, recordings and metadata are
            // stored locally
            log.debug("Static content folder: {}", etFilesPath );
            File[] metadataFiles = new File(etFilesPath)
                    .listFiles((dir, name) -> name.toLowerCase()
                            .endsWith(registryMetadataExtension));
            if (metadataFiles != null) {
                metadataContent = stream(metadataFiles)
                        .map(this::getLocalFileContent).collect(toList());
            }

        } else {
            // If EDM Alluxio is available, recordings and metadata are stored
            // in Alluxio
            List<String> metadataFileList = alluxioService
                    .getMetadataFileList();
            metadataContent = metadataFileList.stream()
                    .map(alluxioService::getFileAsString).collect(toList());

        }
        
        return metadataContent;
    }

    private String getLocalFileContent(File file) {
        String content = "";
        try {
            content = new String(readAllBytes(get(file.getAbsolutePath())));
        } catch (IOException e) {
            String errorMessage = "Exception reading content of " + file
                    + " from Alluxio";
            // Not propagating IOException to improve readability
            throw new EusException(errorMessage, e);
        }
        return content;
    }

}
