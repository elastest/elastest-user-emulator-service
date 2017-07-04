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
package io.elastest.eus.api.service;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service implementation for W3C WebDriver/JSON Wire Protocol.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class WebDriverService {

    private final Logger log = LoggerFactory.getLogger(WebDriverService.class);

    private EpmService epmService;

    @Value("${server.contextPath}")
    private String contextPath;

    private String hubUrl;

    @Autowired
    public WebDriverService(EpmService epmService) {
        this.epmService = epmService;
    }

    public ResponseEntity<String> process(HttpEntity<String> httpEntity,
            HttpServletRequest request) {

        StringBuffer requestUrl = request.getRequestURL();
        String bypassUrl = requestUrl.substring(
                requestUrl.lastIndexOf(contextPath) + contextPath.length());
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        log.debug("{} {}", method, bypassUrl);

        log.trace(">> Request : {}", httpEntity.getBody());

        // Intercept create session
        if (method == POST && bypassUrl.equals("/session")) {
            log.trace("Intercepted POST session");
            hubUrl = epmService
                    .starHubInDockerFromJsonCapabilities(httpEntity.getBody());
            log.trace("Hub URL: {}", hubUrl);
        }

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange = restTemplate
                .exchange(hubUrl + bypassUrl, method, httpEntity, String.class);
        String response = exchange.getBody();

        log.trace("<< Response: {}", response);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(response,
                HttpStatus.OK);

        log.debug("ResponseEntity {}", responseEntity);

        // Intercept destroy session
        if (method == DELETE && bypassUrl.startsWith("/session")
                && countCharsInString(bypassUrl, '/') == 2) {
            log.trace("Intercepted DELETE session");

            epmService.stopHubInDocker();

            // TODO: Implement a timeout mechanism just in case this command is
            // never invoked
        }

        return responseEntity;
    }

    private int countCharsInString(String string, char c) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

}
