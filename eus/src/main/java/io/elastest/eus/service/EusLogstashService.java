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
package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.session.SessionInfo;

/**
 * Logstash service.
 *
 * @since 0.5.0-alpha2
 */
@Service
public class EusLogstashService {
    final Logger log = getLogger(lookup().lookupClass());

    @Value("${et.browser.component.prefix}")
    private String etBrowserComponentPrefix;

    public DynamicDataService dynamicDataService;

    private Map<String, Integer> executionCounterMap = new HashMap<>();
    private Map<String, Integer> executionSessionMap = new HashMap<>();
    private Map<String, Date> mapKeyDateCreationMap = new HashMap<>();

    EusLogstashService(DynamicDataService dynamicDataService) {
        this.dynamicDataService = dynamicDataService;
    }

    // Every 10 min
    // @Scheduled(fixedRate = 600000)
    // public void cleanMaps() {
    // // Clear map key if was stored since more than
    // // 1,5 hours
    // int maxDifference = 5400000;
    // for (Entry<String, Date> entry : mapKeyDateCreationMap.entrySet()) {
    // long difference = entry.getValue().getTime() - new Date().getTime();
    // if (difference > maxDifference) {
    // try {
    // log.debug(
    // "EusLogstashService: Cleaning Execution Map Key {}",
    // entry.getKey());
    // mapKeyDateCreationMap.remove(entry.getKey());
    // executionCounterMap.remove(entry.getKey());
    // executionSessionMap.remove(entry.getKey());
    // } catch (Exception e) {
    // }
    // }
    // }
    // }

    public void sendBrowserConsoleToLogstash(String jsonMessages,
            SessionInfo sessionInfo, String monitoringIndex) {
        String lsHttpApi = dynamicDataService.getLogstashHttpsApi();
        log.trace("lsHttpApi: {} etMonExec: {}", lsHttpApi, monitoringIndex);
        if (lsHttpApi == null || monitoringIndex == null) {
            return;
        }

        try {
            URL url = new URL(lsHttpApi);

            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            String component = getComponent(sessionInfo);

            String body = "{" + "\"component\":\"" + component + "\""
                    + ",\"exec\":\"" + monitoringIndex + "\""
                    + ",\"stream\":\"console\"" + ",\"messages\":"
                    + jsonMessages + "}";
            byte[] out = body.getBytes(UTF_8);
            log.debug("{} => Sending browser log to logstash ({}): {}",
                    sessionInfo.getSessionId(), lsHttpApi, body);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type",
                    "application/json; charset=UTF-8");
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
                os.write(out);
            }
        } catch (Exception e) {
            log.error("Exception in send browser console log trace", e);
        }

    }

    public String getComponent(SessionInfo sessionInfo) {
        String sessionId = sessionInfo.getSessionId();
        String component = etBrowserComponentPrefix;

        // By ElasTest Execution
        ExecutionData etExecData = sessionInfo.getElastestExecutionData();
        if (etExecData != null && etExecData.gettJobId() != null) {
            // Style: tss_eus_browser_tJob_XX_COUNTER(_TESTNAME)

            String key = etExecData.getKey() + "_" + sessionId;

            boolean increase = true;
            if (!executionCounterMap.containsKey(etExecData.getKey())) {
                executionCounterMap.put(etExecData.getKey(), 0);
                mapKeyDateCreationMap.put(etExecData.getKey(), new Date());
                increase = false;
            }

            if (!executionSessionMap.containsKey(key)) {
                int value = executionCounterMap.get(etExecData.getKey());

                if (increase) {
                    value += 1;
                    executionCounterMap.put(etExecData.getKey(), value);
                    mapKeyDateCreationMap.put(etExecData.getKey(), new Date());
                }

                executionSessionMap.put(key, value);
                mapKeyDateCreationMap.put(key, new Date());
            }

            String tJobPrefix = etExecData.getType() + "_"
                    + etExecData.gettJobId();

            component += tJobPrefix + "_" + executionSessionMap.get(key);

            // TestName if is present
            if (sessionInfo.getTestName() != null) {
                component += "_" + sessionInfo.getTestName();
            }

        } else { // Generic
            component += sessionId;
        }

        return component;
    }

    public String getJsonMessageFromValueList(
            List<io.elastest.eus.json.WebDriverLog.Value> values) {
        StringBuilder stringBuilder = new StringBuilder("[");

        int counter = 0;
        for (io.elastest.eus.json.WebDriverLog.Value value : values) {
            stringBuilder.append(formatJsonMessage(value.toString()));
            if (counter < values.size() - 1) {
                stringBuilder.append(",");
            } else {
                stringBuilder.append("]");
            }
            counter++;
        }
        return stringBuilder.toString();
    }

    public static String formatJsonMessage(String msg) {
        // Split new lines and join with new line character
        String[] splittedMsg = msg.split(String.format("%n"));
        msg = String.join("\\n", splittedMsg);
        return "\"" + parseMsg(msg) + "\"";
    }

    public static String parseMsg(String msg) {
        // replace " to \" only if " is not preceded by \
        return msg.replaceAll("(?<!\\\\)\\\"", "\\\\\"");
    }

}
