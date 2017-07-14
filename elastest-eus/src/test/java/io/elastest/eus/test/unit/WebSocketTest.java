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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.app.EusSpringBootApp;
import io.elastest.eus.test.unit.WebSocketClient.MessageHandler;

/**
 * Tests for properties WebSocket.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EusSpringBootApp.class, webEnvironment = RANDOM_PORT)
public class WebSocketTest {

    final Logger log = LoggerFactory.getLogger(WebSocketTest.class);

    @LocalServerPort
    int serverPort;

    @Value("${ws.path}")
    private String wsPath;

    @Value("${server.contextPath}")
    private String contextPath;

    @BeforeEach
    void setup() {
        log.debug("App started on port {}", serverPort);
    }

    @Test
    void test() throws Exception {
        String wsUrl = "ws://localhost:" + serverPort + "/" + contextPath
                + wsPath;

        final String sentMessage = "foo";

        WebSocketClient webSocketClient = new WebSocketClient(wsUrl);
        webSocketClient.addMessageHandler(new MessageHandler() {
            @Override
            public void handleMessage(String receivedMessage) {
                log.debug("Sent message: {} --- received message: {}",
                        sentMessage, receivedMessage);
                assertEquals(sentMessage, receivedMessage);
            }
        });

        webSocketClient.sendMessage(sentMessage);
    }

}
