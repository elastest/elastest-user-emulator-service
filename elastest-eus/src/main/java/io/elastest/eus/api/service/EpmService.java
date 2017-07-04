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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

import io.elastest.eus.api.EusException;

/**
 * Service implementation simulating EPM (ElasTest Platform Manager)
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class EpmService {

    private final Logger log = LoggerFactory.getLogger(EpmService.class);

    private static final String CONTAINER_NAME = "browser-in-docker";
    private static final int WAIT_TIMEOUT = 10; // seconds
    private static final int POLL_TIME = 200; // milliseconds
    private static final int REMOVE_CONTAINER_RETRIES = 10;
    private static final int CONTAINER_HUB_PORT = 4444;

    @Value("${docker.host.ip}")
    private String dockerHostIp;

    private PropertiesService propertiesService;

    private DockerClient dockerClient;

    @Autowired
    public EpmService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    @PostConstruct
    public void postConstruct() {
        dockerClient = DockerClientBuilder.getInstance().build();
    }

    public String starHubInDockerFromJsonCapabilities(String jsonMessage) {
        String imageId = propertiesService.getDockerImageFromJson(jsonMessage);

        log.info("Pulling image {} ... please wait", imageId);

        dockerClient.pullImageCmd(imageId).exec(new PullImageResultCallback())
                .awaitSuccess();

        Ports portBindings = new Ports();
        int bindPort = getFreePort();
        portBindings.bind(ExposedPort.tcp(CONTAINER_HUB_PORT),
                Binding.bindPort(bindPort));

        dockerClient.createContainerCmd(imageId).withPortBindings(portBindings)
                .withName(CONTAINER_NAME).exec();

        dockerClient.startContainerCmd(CONTAINER_NAME).exec();
        waitForContainer(CONTAINER_NAME);

        String hubUrl = "http://" + dockerHostIp + ":" + bindPort + "/wd/hub";
        waitForHostIsReachable(hubUrl);

        return hubUrl;
    }

    private int getFreePort() {
        int port = CONTAINER_HUB_PORT;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        } catch (IOException e) {
            log.warn("Exception looking for a free port", e);
        }
        return port;
    }

    public void stopHubInDocker() {
        stopContainer(CONTAINER_NAME);
        removeContainer(CONTAINER_NAME);
    }

    private void stopContainer(String containerName) {
        if (isRunningContainer(containerName)) {
            log.trace("Stopping container {}", containerName);
            dockerClient.stopContainerCmd(containerName).exec();

        } else {
            log.debug("Container {} is not running", containerName);
        }
    }

    private void removeContainer(String containerName) {
        if (existsContainer(containerName)) {
            log.trace("Removing container {}", containerName);
            boolean removed = false;
            int count = 0;
            do {
                try {
                    count++;
                    dockerClient.removeContainerCmd(containerName)
                            .withRemoveVolumes(true).exec();
                    log.trace("Removed {}", containerName, count);
                    removed = true;

                } catch (Throwable e) {
                    if (count == REMOVE_CONTAINER_RETRIES) {
                        log.error("Exception removing container {}",
                                containerName, e);
                    }
                    try {
                        log.trace("Waiting for removing {} ({} retries)",
                                containerName, count);
                        Thread.sleep(WAIT_TIMEOUT);
                    } catch (InterruptedException e1) {
                        log.warn("Exception waiting to remove container", e1);
                    }
                }
            } while (!removed && count <= REMOVE_CONTAINER_RETRIES);
        }
    }

    private void waitForContainer(String containerName) {
        boolean isRunning = false;

        long timeoutMs = System.currentTimeMillis()
                + SECONDS.toMillis(WAIT_TIMEOUT);
        do {
            isRunning = isRunningContainer(containerName);
            if (!isRunning) {

                // Check timeout
                if (System.currentTimeMillis() > timeoutMs) {
                    throw new EusException("Timeout of " + WAIT_TIMEOUT
                            + " seconds waiting for container "
                            + containerName);
                }

                try {
                    // Wait poll time
                    log.trace(
                            "Container {} is not still running ... waiting {} ms",
                            containerName, POLL_TIME);
                    Thread.sleep(POLL_TIME);

                } catch (InterruptedException e) {
                    log.warn("Exception waiting for hub", e);
                }

            }
        } while (!isRunning);
    }

    private boolean isRunningContainer(String containerName) {
        boolean isRunning = false;
        if (existsContainer(containerName)) {
            isRunning = inspectContainer(containerName).getState().getRunning();
            log.trace("Container {} is running: {}", containerName, isRunning);
        }

        return isRunning;
    }

    private boolean existsContainer(String containerName) {
        boolean exists = true;
        try {
            dockerClient.inspectContainerCmd(containerName).exec();
            log.trace("Container {} already exist", containerName);

        } catch (NotFoundException e) {
            log.trace("Container {} does not exist", containerName);
            exists = false;
        }
        return exists;
    }

    private InspectContainerResponse inspectContainer(String containerName) {
        return dockerClient.inspectContainerCmd(containerName).exec();
    }

    public void waitForHostIsReachable(String url) {
        long timeoutMillis = MILLISECONDS.convert(WAIT_TIMEOUT, SECONDS);
        long endTimeMillis = System.currentTimeMillis() + timeoutMillis;

        log.debug("Waiting for {} to be reachable (timeout {} seconds)", url,
                WAIT_TIMEOUT);

        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs,
                                String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs,
                                String authType) {
                        }
                    } };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            int responseCode = 0;
            while (true) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(
                            url).openConnection();
                    connection.setConnectTimeout((int) timeoutMillis);
                    connection.setReadTimeout((int) timeoutMillis);
                    connection.setRequestMethod("HEAD");
                    responseCode = connection.getResponseCode();

                    break;
                } catch (SSLHandshakeException | SocketException e) {
                    log.trace("Error {} waiting URL {}, trying again in {} ms",
                            e.getMessage(), url, POLL_TIME);

                    // Polling to wait a consistent state
                    Thread.sleep(POLL_TIME);
                }
                if (System.currentTimeMillis() > endTimeMillis) {
                    break;
                }
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.trace("URL " + url + " not reachable. Response code="
                        + responseCode);
            }
        } catch (Exception e) {
            throw new EusException("URL " + url + " not reachable in "
                    + WAIT_TIMEOUT + " seconds", e);
        }

        log.debug("URL {} already reachable", url);
    }

}
