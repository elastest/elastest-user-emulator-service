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
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    EusLogstashService(DynamicDataService dynamicDataService) {
        this.dynamicDataService = dynamicDataService;
    }

    public void sendBrowserConsoleToLogstash(String jsonMessages,
            String sessionId, String monitoringIndex) {
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

            String component = etBrowserComponentPrefix + sessionId;
            String body = "{" + "\"component\":\"" + component + "\""
                    + ",\"exec\":\"" + monitoringIndex + "\""
                    + ",\"stream\":\"console\"" + ",\"messages\":"
                    + jsonMessages + "}";
            byte[] out = body.getBytes(UTF_8);
            log.debug("Sending browser log to logstash ({}): {}", lsHttpApi,
                    body);
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
        return "\"" + parseMsg(msg) + "\"";
    }

    public static String parseMsg(String msg) {
        // replace " to \" only if " is not preceded by \
        return msg.replaceAll("(?<!\\\\)\\\"", "\\\\\"");
    }

}
