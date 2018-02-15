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

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static io.elastest.eus.docker.DockerContainer.dockerBuilder;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;

import io.elastest.eus.docker.DockerContainer.DockerBuilder;
import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for VNC capabilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Service
public class VncService {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${eus.container.prefix}")
    private String eusContainerPrefix;

    @Value("${novnc.container.sufix}")
    private String noVncContainerSufix;

    @Value("${novnc.exposedport}")
    private int noVncExposedPort;

    @Value("${et.docker.img.novnc}")
    private String noVncImageId;

    @Value("${hub.vnc.password}")
    private String hubVncPassword;

    @Value("${novnc.autofocus.html}")
    private String vncAutoFocusHtml;

    @Value("${hub.vnc.exposedport}")
    private int hubVncExposedPort;

    @Value("${use.torm}")
    private boolean useTorm;

    @Value("${docker.network}")
    private String dockerNetwork;

    @Value("${et.host.env}")
    private String etHostEnv;

    private DockerService dockerService;
    SessionService sessionService;

    @Autowired
    public VncService(DockerService dockerService,
            SessionService sessionService) {
        this.dockerService = dockerService;
        this.sessionService = sessionService;
    }

    public void startVncContainer(SessionInfo sessionInfo)
            throws IOException, InterruptedException {
        log.debug("Starting VNC container in session {}",
                sessionInfo.getSessionId());

        String vncContainerName = dockerService.generateContainerName(
                eusContainerPrefix + noVncContainerSufix);

        // Port binding
        int noVncBindPort = dockerService.findRandomOpenPort();
        Binding bindNoVncPort = bindPort(noVncBindPort);
        ExposedPort exposedNoVncPort = tcp(noVncExposedPort);
        List<PortBinding> portBindings = asList(
                new PortBinding(bindNoVncPort, exposedNoVncPort));

        DockerBuilder dockerBuilder = dockerBuilder(noVncImageId,
                vncContainerName).portBindings(portBindings);

        if (useTorm) {
            dockerBuilder.network(dockerNetwork);
        }
        dockerService.startAndWaitContainer(dockerBuilder.build());

        String vncContainerIp = dockerService.getDockerServerIp();
        String hubContainerIp = dockerService.getDockerServerIp();
        int hubVncBindPort = sessionInfo.getHubVncBindPort();

        String vncUrlFormat = "http://%s:%d/" + vncAutoFocusHtml
                + "?host=%s&port=%d&resize=scale&autoconnect=true&password="
                + hubVncPassword;

        String vncUrl = format(vncUrlFormat, vncContainerIp, noVncBindPort,
                hubContainerIp, hubVncBindPort);
        dockerService.waitForHostIsReachable(vncUrl);
        
        String etHost = getenv(etHostEnv);
        if (etHost != null) {
            if (etHost.equalsIgnoreCase("localhost")) {
                vncContainerIp = dockerService
                        .getContainerIpAddress(vncContainerName);
                hubContainerIp = dockerService.getContainerIpAddress(
                        sessionInfo.getHubContainerName());
                vncUrl = format(vncUrlFormat, vncContainerIp, noVncExposedPort,
                        hubContainerIp, hubVncExposedPort);
            } else {
                vncContainerIp = etHost;
                hubContainerIp = etHost;
                vncUrl = format(vncUrlFormat, vncContainerIp, noVncBindPort,
                        hubContainerIp, hubVncBindPort);
            }
        }
        
        

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
