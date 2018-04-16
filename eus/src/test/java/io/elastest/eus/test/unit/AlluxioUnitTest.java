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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.invoke.MethodHandles.lookup;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.internal.util.reflection.FieldSetter;
import org.slf4j.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.elastest.eus.service.AlluxioService;
import io.elastest.eus.test.util.MockitoExtension;

/**
 * Tests for EDM Alluxio.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(PER_CLASS)
@Tag("unit")
@DisplayName("Unit tests for Alluxio Service")
public class AlluxioUnitTest {

    final Logger log = getLogger(lookup().lookupClass());

    @InjectMocks
    AlluxioService alluxioService;

    WireMockServer wireMockServer;

    // Test data
    String filename = "foo";
    String nonExistentFilename = "non-existent-file";
    String streamId = "1";
    String contentFile = "dummy";

    @BeforeAll
    void setup() throws Exception {
        // Look for free port
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        // Mock server for Alluxio
        wireMockServer = new WireMockServer(options().port(port));
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Values injected with Spring properties
        String edmAlluxioUrlFieldName = "edmAlluxioUrl";
        String mockAlluxioUrl = "http://localhost:" + port;
        FieldSetter.setField(alluxioService,
                AlluxioService.class.getDeclaredField(edmAlluxioUrlFieldName),
                mockAlluxioUrl);
        String metadataExtensionFieldName = "metadataExtension";
        String metadataExtension = ".eus";
        FieldSetter.setField(alluxioService, AlluxioService.class
                .getDeclaredField(metadataExtensionFieldName),
                metadataExtension);

        log.debug("Mock servicio for Alluxio in URL {}", mockAlluxioUrl);

        // Stubbing service
        stubFor(post(urlEqualTo("/api/v1/paths//" + filename + "/open-file"))
                .willReturn(aResponse().withStatus(200).withBody(streamId)));
        stubFor(post(urlEqualTo("/api/v1/streams/" + streamId + "/read"))
                .willReturn(aResponse().withStatus(200).withBody(contentFile)));
        stubFor(post(urlEqualTo("/api/v1/streams/" + streamId + "/close"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(post(urlEqualTo("/api/v1/paths//" + filename + "/create-file"))
                .willReturn(aResponse().withStatus(200).withBody(streamId)));
        stubFor(post(urlEqualTo("/api/v1/streams/" + streamId + "/write"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(post(urlEqualTo("/api/v1/paths//" + filename + "/delete"))
                .willReturn(aResponse().withStatus(200)));
        String alluxioListJson = IOUtils.toString(
                this.getClass().getResourceAsStream("/list-alluxio.json"),
                Charset.defaultCharset());
        stubFor(post(urlEqualTo("/api/v1/paths//%2F/list-status")).willReturn(
                aResponse().withStatus(200).withBody(alluxioListJson)));

        alluxioService.postConstruct();
    }

    @Test
    @DisplayName("Get file")
    void testGetFile() throws IOException {
        String response = alluxioService.getFileAsString(filename);
        assertThat(response, equalTo(contentFile));
    }

    @Test
    @DisplayName("Get Non-Existent File")
    void testGetNonExistentFile() throws IOException {
        assertThrows(Exception.class, () -> {
            alluxioService.getFileAsString(nonExistentFilename);
        });
    }

    @Test
    @DisplayName("Write file")
    void testWriteFile() throws IOException {
        alluxioService.writeFile(filename, contentFile.getBytes());
    }

    @Test
    @DisplayName("Delete file")
    void testDeleteFile() throws IOException {
        boolean deleteFileResult = alluxioService.deleteFile(filename);
        assertThat(deleteFileResult, equalTo(true));
    }

    @Test
    @DisplayName("List metatadata")
    void testListMetadataFile() throws IOException {
        List<String> metadataFileList = alluxioService.getMetadataFileList();
        assertThat(metadataFileList, not(empty()));
    }

    @AfterAll
    void teardown() {
        wireMockServer.stop();
    }

}
