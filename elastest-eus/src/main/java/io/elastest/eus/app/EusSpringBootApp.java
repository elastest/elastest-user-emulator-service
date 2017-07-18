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
package io.elastest.eus.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import io.elastest.eus.service.JsonService;
import io.elastest.eus.service.RegistryService;
import io.elastest.eus.service.WebSocketService;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Main class (SpringBootApplication).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@SpringBootApplication
@EnableSwagger2
@EnableWebSocket
@ComponentScan(basePackages = "io.elastest.eus")
public class EusSpringBootApp implements WebSocketConfigurer {

    private final Logger log = LoggerFactory.getLogger(EusSpringBootApp.class);

    private RegistryService registryService;

    private JsonService jsonService;

    @Autowired
    public EusSpringBootApp(RegistryService registryService,
            JsonService jsonService) {
        this.registryService = registryService;
        this.jsonService = jsonService;
    }

    @Value("${ws.path}")
    private String wsPath;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketService(), wsPath).setAllowedOrigins("*");
        log.debug("Registering WebSocker handler at {}", wsPath);
    }

    @Bean
    public WebSocketService webSocketService() {
        return new WebSocketService(registryService, jsonService);
    }

    public static void main(String[] args) throws Exception {
        new SpringApplication(EusSpringBootApp.class).run(args);
    }

}
