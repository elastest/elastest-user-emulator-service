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

import static org.junit.jupiter.api.Assertions.assertThrows;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import io.elastest.eus.service.WebDriverService;
import io.elastest.eus.test.util.MockitoExtension;

/**
 * Tests for shell service.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Unit tests for WebDriverService")
public class WebDriverUnitTest {

    @InjectMocks
    WebDriverService webDriverService;

    @Test
    @DisplayName("Try to get WebRtc Monitoring Local Storage String")
    void testGetWebRtcMonitoringLocalStorageStr() {
        assertThrows(Exception.class, () -> {
            webDriverService.getWebRtcMonitoringLocalStorageStr(
                    "4562f70d-a350-4e88-96da-25d56c91f336", "test_monitoring_index");
        });

    }
}