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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.net.ServerSocket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.elastest.epm.client.service.DockerService;
import io.elastest.eus.config.EusApplicationContextProvider;
import io.elastest.eus.config.EusContextProperties;
import io.elastest.eus.platform.manager.BrowserDockerManager;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.service.EusLogstashService;
import io.elastest.eus.session.SessionManager;
import io.elastest.eus.test.BaseTest;

/**
 * Tests for Logstash service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.1
 */
@TestInstance(PER_CLASS)
@Tag("integration")
@DisplayName("Integration test for Logstash Service")
public class LogstashIntegrationTest extends BaseTest {
    @Autowired
    EusLogstashService logstashService;

    @Autowired
    DockerService dockerService;

    @Autowired
    EusFilesService eusFilesService;

    WireMockServer wireMockServer;

    @BeforeAll
    void setup() throws Exception {
        // Look for free port
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        // Mock server for Logstash
        wireMockServer = new WireMockServer(options().port(port));
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Values injected with Spring properties
        String mockLogstashUrl = "http://localhost:" + port + "/";
        logstashService.dynamicDataService.setLogstashHttpsApi(mockLogstashUrl);

        // Stubbing service
        stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));
    }

    @Test
    @DisplayName("Send dummy console logs to mock logstash")
    void test() {
        EusContextProperties contextProperties = EusApplicationContextProvider
                .getContextPropertiesObject();
        BrowserDockerManager dockerServiceImpl = new BrowserDockerManager(
                dockerService, eusFilesService, contextProperties);
        SessionManager sessionManager = new SessionManager(dockerServiceImpl);
        sessionManager.setSessionId("sessionId");
        logstashService.sendBrowserConsoleToLogstash("{}", sessionManager,
                "normal");
        verify(postRequestedFor(urlEqualTo("/")).withHeader("Content-Type",
                equalTo("application/json; charset=UTF-8")));
    }

    @AfterAll
    void teardown() {
        wireMockServer.stop();
    }

}
