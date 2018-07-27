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
import static org.hamcrest.core.StringContains.containsString;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests for recording service with non-existing Alluxio service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration tests for recording service with non-existing Alluxio service")
public class RecordingIntegrationTest {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${api.context.path}")
    String apiContextPath;

    @LocalServerPort
    int serverPort;

    @Autowired
    WebApplicationContext webContext;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Test
    @DisplayName("POST /session/{sessionId}/recording/hubContainerName/start")
    void testStartRecording() throws Exception {
        String videoName = "video1";

        mockMvc.perform(post(apiContextPath
                + "/session/sessionId/recording/hubContainerName/start")
                        .content(videoName).contentType("text/plain"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /session/{sessionId}/recording/hubContainerName/stop")
    void testStopRecording() throws Exception {
        mockMvc.perform(delete(apiContextPath
                + "/session/sessionId/recording/hubContainerName/stop"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /session/{sessionId}/recording")
    void testGetRecording() throws Exception {
        mockMvc.perform(get(apiContextPath + "/session/sessionId/recording"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sessionId.mp4")));
    }

    @Test
    @DisplayName("DELETE /session/{sessionId}/recording")
    void testDeleteRecording() throws Exception {
        mockMvc.perform(delete(apiContextPath + "/session/sessionId/recording"))
                .andExpect(status().isInternalServerError());
    }

}
