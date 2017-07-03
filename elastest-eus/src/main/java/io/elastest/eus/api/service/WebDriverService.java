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

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @see <a href="https://www.w3.org/TR/webdriver/">W3C WebDriver</a>
 * @see <a href=
 *      "https://github.com/SeleniumHQ/selenium/wiki/JsonWireProtocol">JsonWireProtocol/a>
 */
@Service
public class WebDriverService {

    private final Logger log = LoggerFactory.getLogger(WebDriverService.class);

    @Value("${server.contextPath}")
    private String contextPath;

    public ResponseEntity<String> process(HttpEntity<String> httpEntity,
            HttpServletRequest request) {

        StringBuffer requestUrl = request.getRequestURL();
        String bypassUrl = requestUrl.substring(
                requestUrl.lastIndexOf(contextPath) + contextPath.length());
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        log.debug("[WebDriverService] {} {}", method, bypassUrl);

        // TODO Use Docker instead of local Selenium Server
        String newUrl = "http://localhost:4444/wd/hub" + bypassUrl;
        log.trace("[WebDriverService] >> Request : {}", httpEntity.getBody());

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange = restTemplate.exchange(newUrl, method,
                httpEntity, String.class);
        String response = exchange.getBody();

        log.trace("[WebDriverService] << Response: {}", response);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(response,
                HttpStatus.OK);

        log.debug("[WebDriverService] response {}", responseEntity);

        return responseEntity;
    }

}
