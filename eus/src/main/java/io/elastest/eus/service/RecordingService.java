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

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

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

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for VNC and recording capabilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Service
public class RecordingService {

    private final Logger log = LoggerFactory.getLogger(RecordingService.class);

    @Value("${hub.vnc.exposedport}")
    private int hubVncExposedPort;

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

    @Value("${eus.container.prefix}")
    private String eusContainerPrefix;

    @Value("${novnc.container.sufix}")
    private String noVncContainerSufix;

    @Value("${novnc.exposedport}")
    private int noVncExposedPort;

    @Value("${novnc.image.id}")
    private String noVncImageId;

    @Value("${hub.vnc.password}")
    private String hubVncPassword;

    @Value("${novnc.autofocus.html}")
    private String vncAutoFocusHtml;

    private DockerService dockerService;
    private JsonService jsonService;
    SessionService sessionService;

    @Autowired
    public RecordingService(DockerService dockerService,
            JsonService jsonService, SessionService sessionService) {
        this.dockerService = dockerService;
        this.jsonService = jsonService;
        this.sessionService = sessionService;
    }

    public void startRecording(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String noNvcContainerName = sessionInfo.getVncContainerName();
        String hubContainerIp = dockerService.getDockerServerIp();
        String hubContainerPort = String
                .valueOf(sessionInfo.getHubVncBindPort());

        log.debug("Recording session {} in {}:{}", sessionId, hubContainerIp,
                hubVncExposedPort);

        dockerService.execCommand(noNvcContainerName, false, "flvrec.py", "-P",
                "passwd_file", "-o", sessionId + ".flv", hubContainerIp,
                hubContainerPort);
    }

    public void stopRecording(SessionInfo sessionInfo) {
        String noNvcContainerName = sessionInfo.getVncContainerName();
        log.trace("Stopping recording of container {}", noNvcContainerName);
        dockerService.execCommand(noNvcContainerName, false, "killall",
                "flvrec.py");
    }

    public void storeRecording(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String noNvcContainerName = sessionInfo.getVncContainerName();
        String recordingFileName = sessionId + registryRecordingExtension;

        try {
            // Create recording in container
            dockerService.execCommand(noNvcContainerName, true, "ffmpeg", "-i",
                    sessionId + ".flv", "-c:v", "libx264", "-crf", "19",
                    "-strict", "experimental", recordingFileName);

            // TODO send recording also to alluxio

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

            sessionInfo.setRecordingPath(contextPath + registryContextPath + "/"
                    + recordingFileName);

        } catch (IOException e) {
            log.error("Exception storing recording (sessiodId {})",
                    sessionInfo.getSessionId(), e);
        }
    }

    public void storeMetadata(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String metadataFileName = sessionId + registryMetadataExtension;

        try {
            JSONObject sessionInfoToJson = jsonService
                    .recordedSessionJson(sessionInfo);
            FileUtils.writeStringToFile(
                    new File(registryFolder + metadataFileName),
                    sessionInfoToJson.toString(), Charset.defaultCharset());

            // TODO send also to alluxio

        } catch (IOException e) {
            log.error("Exception storing metadata (sessiodId {})",
                    sessionInfo.getSessionId(), e);
        }
    }

    public void startVncContainer(SessionInfo sessionInfo) {
        log.debug("Starting VNC container in session {}",
                sessionInfo.getSessionId());

        String vncContainerName = dockerService.generateContainerName(
                eusContainerPrefix + noVncContainerSufix);

        // Port binding
        int noVncBindPort = dockerService.findRandomOpenPort();
        Binding bindNoVncPort = Ports.Binding.bindPort(noVncBindPort);
        ExposedPort exposedNoVncPort = ExposedPort.tcp(noVncExposedPort);

        PortBinding[] portBindings = {
                new PortBinding(bindNoVncPort, exposedNoVncPort) };

        dockerService.startAndWaitContainer(noVncImageId, vncContainerName,
                portBindings);

        String vncContainerIp = dockerService.getDockerServerIp();
        String hubContainerIp = dockerService.getDockerServerIp();

        String vncUrl = "http://" + vncContainerIp + ":" + noVncBindPort + "/"
                + vncAutoFocusHtml + "?host=" + hubContainerIp + "&port="
                + sessionInfo.getHubVncBindPort()
                + "&resize=scale&autoconnect=true&password=" + hubVncPassword;

        dockerService.waitForHostIsReachable(vncUrl);

        sessionInfo.setVncContainerName(vncContainerName);
        sessionInfo.setVncUrl(vncUrl);
        sessionInfo.setNoVncBindPort(noVncBindPort);

        startRecording(sessionInfo);
    }

    public ResponseEntity<String> getVnc(String sessionId) {
        SessionInfo sessionInfo = sessionService.getSession(sessionId);
        if (sessionInfo == null) {
            return sessionService.sessionNotFound();
        }

        ResponseEntity<String> responseEntity = new ResponseEntity<>(
                sessionInfo.getVncUrl(), OK);
        return responseEntity;
    }

    public ResponseEntity<String> deleteVnc(String sessionId) {
        log.debug("Deleting VNC recording of session {}", sessionId);
        String recordingFileName = sessionId + registryRecordingExtension;
        String metadataFileName = sessionId + registryMetadataExtension;

        boolean deleteRecording = new File(registryFolder + recordingFileName)
                .delete();
        boolean deleteMetadata = new File(registryFolder + metadataFileName)
                .delete();

        // TODO delete also from alluxio

        HttpStatus status = deleteRecording && deleteMetadata ? OK
                : INTERNAL_SERVER_ERROR;
        ResponseEntity<String> responseEntity = new ResponseEntity<>(status);
        log.debug("... response {}", status);
        return responseEntity;
    }

}
