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

import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.external.DockerComposeProject;
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

    final Logger log = LoggerFactory
            .getLogger(DockerComposeIntegrationTest.class);

    @Autowired
    private DockerComposeService dockerComposeService;

    @Test
    @DisplayName("Start and stop Docker compose")
    void testStartAndStop() throws IOException {
        // Test data
        String projectName = "elastesteus";
        String dockerComposeYml = IOUtils.toString(
                this.getClass().getResourceAsStream("/docker-compose.yml"),
                defaultCharset());

        // Start docker compose
        DockerComposeProject dockerComposeProject = dockerComposeService
                .createAndStartDockerComposeProject(projectName,
                        dockerComposeYml);
        assertThat(dockerComposeProject.isStarted(), equalTo(true));

        // List projects and stop all of them
        List<DockerComposeProject> projects = dockerComposeService
                .listProjects();
        for (DockerComposeProject project : projects) {
            project.stop();
            assertThat(project.isStarted(), equalTo(false));
            assertThat(project.getDockerComposeYml(), not(isEmptyString()));
        }
    }

    @ParameterizedTest(name = "Checking {0}")
    @DisplayName("Invalid project names")
    @ValueSource(strings = { "elastest-eus", "elastest_eus", "€lastest" })
    void testListProjects(String projectName) {
        assertThrows(AssertionError.class, () -> {
            dockerComposeService.createAndStartDockerComposeProject(projectName,
                    null);
        });
    }

}
