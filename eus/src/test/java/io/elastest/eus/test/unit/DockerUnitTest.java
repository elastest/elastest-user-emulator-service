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
package io.elastest.eus.test.unit;

import static com.github.dockerjava.api.model.ExposedPort.tcp;
import static com.github.dockerjava.api.model.Ports.Binding.bindPort;
import static io.elastest.eus.docker.DockerContainer.dockerBuilder;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;

import io.elastest.eus.service.DockerService;
import io.elastest.eus.service.ShellService;
import io.elastest.eus.test.util.MockitoExtension;

/**
 * Tests for shell service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Unit tests for Docker")
public class DockerUnitTest {

    @InjectMocks
    DockerService dockerService;

    @Mock
    ShellService shellService;

    @Test
    @DisplayName("Try to start a container with invalid input")
    void testEmptyContainer() {
        assertThrows(Exception.class, () -> {
            Binding bindPort = bindPort(0);
            ExposedPort exposedPort = tcp(0);
            List<PortBinding> portBindings = asList(
                    new PortBinding(bindPort, exposedPort));

            dockerService.startAndWaitContainer(
                    dockerBuilder("", "").portBindings(portBindings).build());
        });

    }

}