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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

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

import io.elastest.eus.json.WebSocketNewSession;
import io.elastest.eus.json.WebSocketRecordedSession;
import io.elastest.eus.service.EusJsonService;
import io.elastest.eus.service.SessionService;
import io.elastest.eus.session.SessionInfo;
import io.elastest.eus.test.util.WebSocketClient;
import io.elastest.eus.test.util.WebSocketClient.MessageHandler;

/**
 * Tests for properties WebSocket.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration tests for WebSockets")
public class WebSocketIntegrationTest {

    final Logger log = getLogger(lookup().lookupClass());

    @LocalServerPort
    int serverPort;

    @Value("${ws.path}")
    private String wsPath;

    @Value("${ws.protocol.getSessions}")
    private String wsProtocolGetSessions;

    @Value("${ws.protocol.getRecordings}")
    private String wsProtocolGetRecordings;

    @Value("${ws.protocol.newSession}")
    private String wsProtocolNewSession;

    @Value("${ws.protocol.recordedSession}")
    private String wsProtocolRecordedSesssion;

    @Value("${registry.metadata.extension}")
    private String registryMetadataExtension;

    @Value("${registry.folder}")
    private String registryFolder;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private EusJsonService jsonService;

    @BeforeEach
    void setup() {
        log.debug("App started on port {}", serverPort);
    }

    @Test
    @DisplayName("Tests messages about a session through WebSocket")
    void testSessions() throws Exception {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setBrowser("chrome");
        sessionInfo.setVersion("59");
        sessionService.putSession("my-session-id", sessionInfo);

        String jsonMessage = jsonService.objectToJson(sessionInfo);
        assertNotNull(jsonMessage);

        String wsUrl = "ws://localhost:" + serverPort + wsPath;

        final String sentMessage = wsProtocolGetSessions;
        final String[] receivedMessage = { "" };

        CountDownLatch latch = new CountDownLatch(1);
        WebSocketClient webSocketClient = new WebSocketClient(wsUrl);
        webSocketClient.addMessageHandler(new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                log.debug("Sent message: {} -- received message: {}",
                        sentMessage, message);
                receivedMessage[0] = message;
                latch.countDown();
            }
        });

        webSocketClient.sendMessage(sentMessage);

        latch.await(5, SECONDS);

        assertTrue(receivedMessage[0].contains(wsProtocolNewSession));
        webSocketClient.closeSession();
    }

    @Test
    @DisplayName("Tests recording messages through WebSocket")
    void testRecordings() throws Exception {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setBrowser("chrome");
        sessionInfo.setVersion("65");
        String sessionId = "my-session-id";
        sessionService.putSession(sessionId, sessionInfo);

        String jsonFileName = sessionId + registryMetadataExtension;

        String sessionInfoToJson = jsonService
                .objectToJson(new WebSocketRecordedSession(sessionInfo));
        try {
            File dir = new File(registryFolder);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new Exception("The " + registryFolder
                            + " directory could not be created");
                }
            }
        } catch (Exception e) {
            log.debug("Error on create {} file", jsonFileName, e);
        }

        File file = new File(registryFolder + jsonFileName);
        log.debug("Saving {} file into {} folder", jsonFileName,
                registryFolder);

        writeStringToFile(file, sessionInfoToJson, Charset.defaultCharset());

        String jsonMessage = jsonService
                .objectToJson(new WebSocketNewSession(sessionInfo));
        assertNotNull(jsonMessage);

        String wsUrl = "ws://localhost:" + serverPort + wsPath;

        final String sentMessage = wsProtocolGetRecordings;
        final String[] receivedMessage = { "" };

        CountDownLatch latch = new CountDownLatch(1);
        WebSocketClient webSocketClient = new WebSocketClient(wsUrl);
        webSocketClient.addMessageHandler(new MessageHandler() {
            @Override
            public void handleMessage(String message) {
                log.debug("Sent message: {} -- received message: {}",
                        sentMessage, message);
                receivedMessage[0] = message;
                latch.countDown();
            }
        });

        webSocketClient.sendMessage(sentMessage);

        latch.await(5, SECONDS);

        assertTrue(receivedMessage[0].contains(wsProtocolRecordedSesssion));

        file.delete();
        webSocketClient.closeSession();
    }

}
