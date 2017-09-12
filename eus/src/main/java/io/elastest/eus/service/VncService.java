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

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for VNC capabilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Service
public class VncService {

    private final Logger log = LoggerFactory.getLogger(VncService.class);

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
    SessionService sessionService;

    @Autowired
    public VncService(DockerService dockerService,
            SessionService sessionService) {
        this.dockerService = dockerService;
        this.sessionService = sessionService;
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
    }

    public ResponseEntity<String> getVnc(String sessionId) {
        Optional<SessionInfo> sessionInfo = sessionService
                .getSession(sessionId);
        ResponseEntity<String> responseEntity;
        if (sessionInfo.isPresent()) {
            responseEntity = new ResponseEntity<>(sessionInfo.get().getVncUrl(),
                    OK);
        } else {
            responseEntity = new ResponseEntity<>(NOT_FOUND);
            log.debug("<< Response: {} ", responseEntity.getStatusCode());
        }

        return responseEntity;
    }

}
