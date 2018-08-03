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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.elastest.eus.api.model.Event;
import io.elastest.eus.api.model.Latency;
import io.elastest.eus.api.model.Quality;
import io.elastest.eus.api.model.UserMedia;
import io.elastest.eus.service.EusJsonService;

/**
 * Integration tests for non-implemented service operations.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration tests for service operations")
public class ApiServiceIntegrationTest {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${api.context.path}")
    String apiContextPath;

    @Autowired
    WebApplicationContext webContext;

    @Autowired
    EusJsonService jsonService;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    @Test
    @DisplayName("GET /session/{sessionId}/event/{subscriptionId}")
    void testGetSubscription() throws Exception {
        mockMvc.perform(
                get(apiContextPath + "/session/sessionId/event/subscriptionId"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /session/{sessionId}/event/{subscriptionId}")
    void testDeleteSubscription() throws Exception {
        mockMvc.perform(delete(
                apiContextPath + "/session/sessionId/event/subscriptionId"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /session/{sessionId}/element/{elementId}/color")
    void testGetColor() throws Exception {
        mockMvc.perform(get(
                apiContextPath + "/session/sessionId/element/elementId/color"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /session/{sessionId}/element/{elementId}/audio")
    void testGetAudio() throws Exception {
        mockMvc.perform(get(
                apiContextPath + "/session/sessionId/element/elementId/audio"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /session/{sessionId}/stats")
    void testGetStats() throws Exception {
        mockMvc.perform(get(apiContextPath + "/session/sessionId/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /status")
    void testGetStatus() throws Exception {
        mockMvc.perform(get(apiContextPath + "/status"))
                .andExpect(status().isOk()).andExpect(content()
                        .contentType("application/json;charset=UTF-8"));
    }

    @Test
    @DisplayName("POST /session/{sessionId}/usermedia")
    void testPostUsermedia() throws Exception {
        String usermedia = jsonService.objectToJson(new UserMedia());
        mockMvc.perform(post(apiContextPath + "/session/sessionId/usermedia")
                .content(usermedia).contentType("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /session/{sessionId}/element/{elementId}/latency")
    void testPostLatency() throws Exception {
        String latency = jsonService.objectToJson(new Latency());
        mockMvc.perform(post(
                apiContextPath + "/session/sessionId/element/elementId/latency")
                        .content(latency).contentType("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /session/{sessionId}/element/{elementId}/quality")
    void testPostQuality() throws Exception {
        String quality = jsonService.objectToJson(new Quality());
        mockMvc.perform(post(
                apiContextPath + "/session/sessionId/element/elementId/quality")
                        .content(quality).contentType("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /session/{sessionId}/element/{elementId}/event")
    void testPostEvent() throws Exception {
        String event = jsonService.objectToJson(new Event());
        mockMvc.perform(post(
                apiContextPath + "/session/sessionId/element/elementId/event")
                        .content(event).contentType("application/json"))
                .andExpect(status().isOk());
    }

}
