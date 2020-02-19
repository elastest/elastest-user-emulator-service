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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.session.SessionManager;

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

    public String getComponent(SessionManager sessionInfo) {
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

            String tJobPrefix = etExecData.getType() + "_" + etExecData.gettJobId();

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

    public String getIso8601String() {
        return "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    }

    public DateFormat getIso8601DateFormat() {
        return new SimpleDateFormat(getIso8601String());
    }

    public Date getIso8601DateFromStr(String timestamp, TimeZone timezone) throws ParseException {
        DateFormat df = getIso8601DateFormat();
        df.setTimeZone(timezone);

        return df.parse(timestamp);
    }

    public Date getIso8601UTCDateFromStr(String timestamp) throws ParseException {
        return this.getIso8601DateFromStr(timestamp, TimeZone.getTimeZone("GMT-0"));
    }

    public String getIso8601UTCStrFromDate(Date date) throws ParseException {
        DateFormat df = new SimpleDateFormat(getIso8601String(), Locale.UK);
        df.setTimeZone(TimeZone.getTimeZone("GMT-0"));
        return df.format(date);
    }

    public void sendMonitoringTrace(String lsHttpApi, String body) throws Exception {
        URL url = new URL(lsHttpApi);

        log.debug("Sending monitoring trace to {}: {}", url, body);

        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        byte[] out = body.getBytes(UTF_8);

        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }
    }

    /* **************************** */
    /* ******* Send methods ******* */
    /* **************************** */

    public void sendBrowserConsoleToLogstash(String jsonMessages, SessionManager sessionManager,
            String monitoringIndex) {
        String lsHttpApi = dynamicDataService.getLogstashHttpsApi();
        log.trace("lsHttpApi: {} etMonExec: {}", lsHttpApi, monitoringIndex);
        if (lsHttpApi == null || monitoringIndex == null) {
            return;
        }

        try {

            String component = getComponent(sessionManager);

            String body = "{" + "\"component\":\"" + component + "\"" + ",\"exec\":\""
                    + monitoringIndex + "\"" + ",\"stream\":\"console\"" + ",\"messages\":"
                    + jsonMessages + "}";
            log.debug("{} => Sending browser log to logstash ({}): {}",
                    sessionManager.getSessionId(), lsHttpApi, body);
            sendMonitoringTrace(lsHttpApi, body);
        } catch (Exception e) {
            log.error("Exception in send browser console log trace", e);
        }

    }

    public void sendAtomicMetric(SessionManager sessionManager, String metricName, String unit,
            String value, String stream, String timestamp, String monitoringIndex)
            throws Exception {
        String lsHttpApi = dynamicDataService.getLogstashHttpsApi();
        log.trace("lsHttpApi: {} etMonExec: {}", lsHttpApi, monitoringIndex);
        if (lsHttpApi == null || monitoringIndex == null) {
            return;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("component", getComponent(sessionManager));
        jsonObject.addProperty("exec", monitoringIndex);
        jsonObject.addProperty("et_type", metricName);
        jsonObject.addProperty("stream", stream);
        jsonObject.addProperty("stream_type", "atomic_metric");
        jsonObject.addProperty("unit", unit);
        jsonObject.addProperty("metricName", metricName);
        jsonObject.addProperty(metricName, value);
        jsonObject.addProperty("@timestamp", timestamp);

        sendMonitoringTrace(lsHttpApi, jsonObject.toString());
    }

    public void sendAtomicMetric(SessionManager sessionManager, String metricName, String unit,
            String value, String stream, long timestampMillis, String monitoringIndex)
            throws Exception {
        Date timestampAsDate = new Date(timestampMillis);
        String timestamp = getIso8601UTCStrFromDate(timestampAsDate);

        sendAtomicMetric(sessionManager, metricName, unit, value, stream, timestamp,
                monitoringIndex);
    }

}
