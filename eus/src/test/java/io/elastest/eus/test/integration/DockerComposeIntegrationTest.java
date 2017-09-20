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
package io.elastest.eus.test.integration;

import static java.lang.invoke.MethodHandles.lookup;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.external.DockerComposeProject;
import io.elastest.eus.json.DockerContainerInfo;
import io.elastest.eus.service.DockerComposeService;

/**
 * Tests for Docker Compose service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration test for Docker Compose Service")
public class DockerComposeIntegrationTest {

    final Logger log = getLogger(lookup().lookupClass());

    @Autowired
    private DockerComposeService dockerComposeService;

    @Test
    @DisplayName("Start and stop Docker compose")
    void testStartAndStop() throws IOException {
        // Test data
        String projectName = "elastest-eus";
        String dockerComposeFile = "docker-compose.yml";

        // Start docker compose
        DockerComposeProject dockerComposeProject = dockerComposeService
                .createAndStartDockerComposeWithFile(projectName,
                        dockerComposeFile);
        assertThat(dockerComposeProject.isStarted(), equalTo(true));

        // Get container info
        DockerContainerInfo containersInfo = dockerComposeProject
                .getContainersInfo();
        log.debug("Container info {}", containersInfo);
        assertThat(containersInfo.getContainers(), not(empty()));

        // List projects and assert YML content
        List<DockerComposeProject> projects = dockerComposeService
                .listProjects();
        for (DockerComposeProject project : projects) {
            assertThat(project.getDockerComposeYml(), not(isEmptyString()));
        }

        // Stop project
        dockerComposeProject.stop();
        assertThat(dockerComposeProject.isStarted(), equalTo(false));
    }

}
