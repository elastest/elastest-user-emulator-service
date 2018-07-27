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
import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.elastest.eus.service.EusJsonService;

/**
 * Tests for JQ service (JSON processor).
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.1
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration")
@DisplayName("Integration tests using JQ (JSON processor)")
public class JqIntegrationTest {

    @Autowired
    private EusJsonService jsonService;

    @Test
    @DisplayName("JQ test version test")
    void testJq() throws IOException {
        String version = "57.0";
        String json = "{ \"desiredCapabilities\":{  \n"
                + "      \"acceptInsecureCerts\":true,\n"
                + "      \"browserName\":\"firefox\",\n"
                + "      \"platform\":\"ANY\",\n" + "      \"version\":\""
                + version + "\"\n" + "   } }";
        String jq = "walk(if type == \"object\" and .version then .version=\"\" else . end)";
        String result = jsonService.processJsonWithJq(json, jq);

        assertThat(result, not(containsString(version)));
    }

}
