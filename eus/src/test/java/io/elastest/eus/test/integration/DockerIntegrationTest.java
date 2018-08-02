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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.spotify.docker.client.messages.PortBinding;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.service.DockerService;
import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.service.DockerHubService;
import io.elastest.eus.service.EusJsonService;

/**
 * Tests for Docker service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration test for Docker Service")
public class DockerIntegrationTest {

    final Logger log = getLogger(lookup().lookupClass());

    @Autowired
    private DockerService dockerService;

    @Autowired
    private DockerHubService dockerHubService;

    @Autowired
    private EusJsonService jsonService;

    @Value("${hub.exposedport}")
    private int hubExposedPort;

    @Value("${hub.vnc.exposedport}")
    private int hubVncExposedPort;

    @Value("${use.torm}")
    private boolean useTorm;

    @Value("${docker.network}")
    private String dockerNetwork;

    @Test
    @DisplayName("Ask for Chrome to Docker")
    void testDocker() throws Exception {
        // Test data (input)
        String jsonCapabilities = "{ \"desiredCapabilities\": {"
                + "\"browserName\": \"chrome\"," + " \"version\": \"\","
                + "\"platform\": \"ANY\"" + " }" + "}";

        // Exercise
        String browserName = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getBrowserName();
        String version = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getVersion();
        String platform = jsonService
                .jsonToObject(jsonCapabilities, WebDriverCapabilities.class)
                .getDesiredCapabilities().getPlatform();

        log.debug("Starting Hub from JSON message {}", jsonCapabilities);

        String imageId = dockerHubService.getBrowserImageFromCapabilities(
                browserName, version, platform);

        String containerName = dockerService
                .generateContainerName("eus-hub-for-test-");

        Map<String, List<PortBinding>> portBindings = new HashMap<>();

        int hubBindPort = dockerService.findRandomOpenPort();
        String exposedHubPort = Integer.toString(hubExposedPort);
        portBindings.put(exposedHubPort,
                Arrays.asList(PortBinding.of("0.0.0.0", hubBindPort)));

        int hubVncBindPort = dockerService.findRandomOpenPort();
        String exposedHubVncPort = Integer.toString(hubVncExposedPort);
        portBindings.put(exposedHubVncPort,
                Arrays.asList(PortBinding.of("0.0.0.0", hubVncBindPort)));

        DockerBuilder dockerBuilder = new DockerBuilder(imageId);
        dockerBuilder.containerName(containerName);
        dockerBuilder.portBindings(portBindings);
        if (useTorm) {
            dockerBuilder.network(dockerNetwork);
        }

        dockerService.createAndStartContainerWithPull(dockerBuilder.build(),
                false, true);

        // Assertions
        assertTrue(dockerService.existsContainer(containerName));

        // Tear down
        log.debug("Stoping Hub");
        dockerService.stopAndRemoveContainer(containerName);
    }

}
