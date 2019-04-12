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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.elastest.eus.service.DockerHubService;
import io.elastest.eus.test.BaseTest;

/**
 * Tests for Docker Hub service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.1
 */

@Tag("integration")
@DisplayName("Integration test for Docker Hub Service")
public class DockerHubIntegrationTest extends BaseTest {
    @Autowired
    private DockerHubService dockerHubService;

    @Test
    @DisplayName("Get Selenoid browser images from Docker Hub")
    void testBrowserMap() throws IOException {
        Map<String, List<String>> browsers = dockerHubService
                .getBrowsers(false);
        log.debug("Browser map {}", browsers);

        assertThat(browsers.get("chrome"), not(empty()));
        assertThat(browsers.get("firefox"), not(empty()));
        assertThat(browsers.get("opera"), not(empty()));
    }

}
