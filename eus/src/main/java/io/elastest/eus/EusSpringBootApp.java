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
package io.elastest.eus;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import io.elastest.eus.config.EusApplicationContextProvider;
import io.elastest.eus.service.AlluxioService;
import io.elastest.eus.service.EusJsonService;
import io.elastest.eus.service.QoEService;
import io.elastest.eus.service.RecordingService;
import io.elastest.eus.service.SessionService;
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
@ComponentScan(basePackages = { "io.elastest.eus", "io.elastest.epm.client" })
public class EusSpringBootApp implements WebSocketConfigurer {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${ws.path}")
    private String wsPath;

    @Autowired
    private EusJsonService eusJsonService;
    @Autowired
    AlluxioService alluxioService;

    @Bean("eusContext")
    @Primary
    public EusApplicationContextProvider getEusApplicationContextProvider() {
        return new EusApplicationContextProvider();
    }

    @Bean
    @DependsOn({ "eusContext" })
    public QoEService getQoEService() {
        return new QoEService();
    }

    @Bean
    public RecordingService getRecordingService() {
        return new RecordingService(eusJsonService, alluxioService);
    }

    @Bean
    @Primary
    public SessionService getSessionService() {
        return new SessionService(eusJsonService, getRecordingService());

    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(getSessionService(), wsPath).setAllowedOrigins("*");
        log.debug("Registering WebSocker handler at {}", wsPath);
    }

    public static void main(String[] args) {
        new SpringApplication(EusSpringBootApp.class).run(args);
    }

}
