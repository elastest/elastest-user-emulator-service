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

import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.elastest.eus.json.WebDriverLog;
import io.elastest.eus.session.SessionInfo;

/**
 * Service for timeout.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.0-alpha2
 */
@Service
public class TimeoutService {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${log.executor.size}")
    private int logExecutorSize;

    @Value("${log.poll.ms}")
    private int logPollMs;

    private Map<String, Future<?>> logFutureMap;
    private ExecutorService logExecutor;
    private ScheduledExecutorService timeoutExecutor = newScheduledThreadPool(
            1);

    private EusLogstashService logstashService;

    @Autowired
    public TimeoutService(EusLogstashService logstashService) {
        this.logstashService = logstashService;
    }

    @PostConstruct
    public void init() {
        logExecutor = Executors.newFixedThreadPool(logExecutorSize);
        logFutureMap = new HashMap<>(logExecutorSize);
    }

    @PreDestroy
    public void cleanUp() {
        logExecutor.shutdown();
    }

    public void launchLogMonitor(String postUrl, String sessionId, String monitoringIndex) {
        if (!logFutureMap.containsKey(sessionId)) {
            log.info("Launching log monitor using URL {}", postUrl);
            logFutureMap.put(sessionId, logExecutor.submit(() -> {
                RestTemplate restTemplate = new RestTemplate();
                while (true) {
                    try {
                        WebDriverLog response = restTemplate.postForEntity(
                                postUrl, "{\"type\":\"browser\"} ",
                                WebDriverLog.class).getBody();
                        if (!response.getValue().isEmpty()) {
                            String jsonMessages = logstashService
                                    .getJsonMessageFromValueList(
                                            response.getValue());
                            logstashService.sendBrowserConsoleToLogstash(
                                    jsonMessages, sessionId, monitoringIndex);
                        }
                        sleep(logPollMs);

                    } catch (Exception e) {
                        log.trace("Termimating log monitor due to {}",
                                e.getMessage());
                        break;
                    }
                }
            }));
        }
    }

    public void startSessionTimer(SessionInfo sessionInfo, int timeout,
            Runnable deleteSession) {
        if (sessionInfo != null) {
            Future<?> timeoutFuture = timeoutExecutor.schedule(deleteSession,
                    timeout, SECONDS);
            sessionInfo.addTimeoutFuture(timeoutFuture);
            sessionInfo.setTimeout(timeout);
            log.trace("Starting timer in session {} (future {}) ({} seconds)",
                    sessionInfo.getSessionId(), timeoutFuture, timeout);
        }
    }

    public void shutdownSessionTimer(SessionInfo sessionInfo) {
        if (sessionInfo != null) {
            sessionInfo.getTimeoutFutures().forEach(timeoutFuture -> {
                if (timeoutFuture != null) {
                    timeoutFuture.cancel(true);
                    log.trace("Canceling timer in session {} (future {}) ",
                            sessionInfo.getSessionId(), timeoutFuture);
                }
            });
            sessionInfo.getTimeoutFutures().clear();
        }
    }

}
