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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
    @DisplayName("Docker Compose test")
    void testDockerCompose() throws IOException {
        String projectName = "elastest-eus";
        boolean createProject = dockerComposeService.createProject(projectName,
                "version: '2.1'\nservices:\n   elastest-eus:\n      image: elastest/eus\n      environment:\n         - USE_TORM=false\n      expose:\n         - 8040\n      ports:\n         - 8040:8040");
        assertThat(createProject, equalTo(true));

        boolean startProject = dockerComposeService.startProject(projectName);
        assertThat(startProject, equalTo(true));

        boolean stopProject = dockerComposeService.stopProject(projectName);
        assertThat(stopProject, equalTo(true));
    }

}
