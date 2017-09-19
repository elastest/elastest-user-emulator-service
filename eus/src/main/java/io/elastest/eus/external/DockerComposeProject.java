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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.elastest.eus.service.DockerComposeService;

/**
 * Project on docker-compose-ui.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public class DockerComposeProject {

    private final Logger log = LoggerFactory
            .getLogger(DockerComposeProject.class);

    private String projectName;
    private String dockerComposeYml;
    private DockerComposeService dockerComposeService;
    private boolean isStarted = false;

    public DockerComposeProject(String projectName, String dockerComposeYml,
            DockerComposeService dockerComposeService) throws IOException {
        this.projectName = projectName;
        this.dockerComposeYml = dockerComposeYml;
        this.dockerComposeService = dockerComposeService;

        // Auto start in constructor
        this.start();
    }

    public DockerComposeProject(String projectName,
            DockerComposeService dockerComposeService) throws IOException {
        this.projectName = projectName;
        this.dockerComposeYml = dockerComposeService.getYaml(projectName);
        this.dockerComposeService = dockerComposeService;
        isStarted = true;
    }

    public synchronized void start() throws IOException {
        if (!isStarted) {
            log.debug("Starting Docker Compose project {}", projectName);
            dockerComposeService.createProject(projectName, dockerComposeYml);
            dockerComposeService.startProject(projectName);
            isStarted = true;
            log.debug("Docker Compose project {} started correctly",
                    projectName);
        } else {
            log.debug("Docker Compose project {} already started", projectName);
        }
    }

    public synchronized void stop() throws IOException {
        if (isStarted) {
            log.debug("Stopping Docker Compose project {}", projectName);
            dockerComposeService.stopProject(projectName);
            isStarted = false;
            log.debug("Docker Compose project {} stopped correctly",
                    projectName);
        } else {
            log.debug("Docker Compose project {} already is NOT started",
                    projectName);
        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    public String getDockerComposeYml() {
        return dockerComposeYml;
    }

}
