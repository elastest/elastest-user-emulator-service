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

import static com.github.dockerjava.api.model.Capability.SYS_ADMIN;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.lang.invoke.MethodHandles.lookup;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

import io.elastest.eus.docker.DockerContainer;
import io.elastest.eus.docker.DockerException;

/**
 * Service implementation simulating EPM (ElasTest Platform Manager) with
 * Docker.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class DockerService {

    final Logger log = getLogger(lookup().lookupClass());

    @Value("${docker.wait.timeout.sec}")
    private int dockerWaitTimeoutSec;

    @Value("${docker.poll.time.ms}")
    private int dockerPollTimeMs;

    @Value("${docker.server.url:#{null}}")
    private String dockerServerUrl;

    @Value("${docker.default.host.ip}")
    private String dockerDefaultHostIp;

    @Value("${docker.max.route.connections}")
    private int dockerMaxRouteConnections;

    private ShellService shellService;

    private DockerClient dockerClient;
    private String dockerServerIp;
    private boolean isRunningInContainer = false;
    private boolean containerCheked = false;

    public DockerService(ShellService shellService) {
        this.shellService = shellService;
    }

    @PostConstruct
    @SuppressWarnings("resource")
    private void postConstruct() {
        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withMaxPerRouteConnections(dockerMaxRouteConnections);

        DockerClientBuilder dockerClientBuilder = DockerClientBuilder
                .getInstance();
        if (dockerServerUrl != null && !dockerServerUrl.isEmpty()) {
            dockerClientBuilder = DockerClientBuilder
                    .getInstance(dockerServerUrl);
        }
        dockerClient = dockerClientBuilder
                .withDockerCmdExecFactory(dockerCmdExecFactory).build();
    }

    @PreDestroy
    private void teardown() throws IOException {
        dockerClient.close();
    }

    public String getDockerServerUrl() {
        return dockerServerUrl;
    }

    public String getDockerServerIp() throws IOException {
        if (dockerServerIp == null) {
            if (IS_OS_WINDOWS) {
                dockerServerIp = getDockerMachineIp();
            } else {
                if (!containerCheked) {
                    isRunningInContainer = shellService.isRunningInContainer();
                    containerCheked = true;
                }
                if (isRunningInContainer) {
                    dockerServerIp = getContainerIp();

                } else {
                    dockerServerIp = dockerDefaultHostIp;
                }
            }
            log.trace("Docker server IP: {}", dockerServerIp);
        }

        return dockerServerIp;
    }

    public String getContainerIp() throws IOException {
        String ipRoute = shellService.runAndWait("sh", "-c", "/sbin/ip route");
        String[] tokens = ipRoute.split("\\s");
        return tokens[2];
    }

    public String getDockerMachineIp() throws IOException {
        return shellService.runAndWait("docker-machine", "ip")
                .replaceAll("\\r", "").replaceAll("\\n", "");
    }

    public void startAndWaitContainer(DockerContainer dockerContainer)
            throws InterruptedException {
        String containerName = dockerContainer.getContainerName();
        String imageId = dockerContainer.getImageId();

        if (!isRunningContainer(containerName)) {
            pullImageIfNecessary(imageId);

            try (CreateContainerCmd createContainer = dockerClient
                    .createContainerCmd(imageId).withName(containerName)) {

                Optional<String> network = dockerContainer.getNetwork();
                if (network.isPresent()) {
                    log.trace("Using network: {}", network.get());
                    createContainer.withNetworkMode(network.get());
                }
                Optional<List<PortBinding>> portBindings = dockerContainer
                        .getPortBindings();
                if (portBindings.isPresent()) {
                    log.trace("Using port binding: {}", portBindings.get());
                    createContainer.withPortBindings(portBindings.get());
                }
                Optional<List<Volume>> volumes = dockerContainer.getVolumes();
                if (volumes.isPresent()) {
                    log.trace("Using volumes: {}", volumes.get());
                    createContainer.withVolumes(volumes.get());
                }
                Optional<List<Bind>> binds = dockerContainer.getBinds();
                if (binds.isPresent()) {
                    log.trace("Using binds: {}", binds.get());
                    createContainer.withBinds(binds.get());
                }
                Optional<List<String>> envs = dockerContainer.getEnvs();
                if (envs.isPresent()) {
                    log.trace("Using envs: {}", envs.get());
                    createContainer.withEnv(envs.get());
                }
                Optional<List<String>> cmd = dockerContainer.getCmd();
                if (cmd.isPresent()) {
                    log.trace("Using cmd: {}", cmd.get());
                    createContainer.withCmd(cmd.get());
                }

                Optional<Long> shmSize = dockerContainer.getShmSize();
                if (shmSize.isPresent()) {
                    HostConfig hostConfig = createContainer.getHostConfig();
                    hostConfig.withShmSize(shmSize.get());
                    createContainer.withHostConfig(hostConfig);
                }

                createContainer.withCapAdd(SYS_ADMIN);
                createContainer.exec();
                dockerClient.startContainerCmd(containerName).exec();
                waitForContainer(containerName);
            }
        } else {
            log.warn("Container {} already running", containerName);
        }
    }

    public void pullImageIfNecessary(String imageId) {
        if (!existsImage(imageId)) {
            log.info("Pulling Docker image {} ... please wait", imageId);
            dockerClient.pullImageCmd(imageId)
                    .exec(new PullImageResultCallback()).awaitSuccess();
            log.debug("Docker image {} downloaded", imageId);
        }
    }

    public boolean existsImage(String imageId) {
        boolean exists = true;
        try {
            dockerClient.inspectImageCmd(imageId).exec();
            log.debug("Docker image {} already exists", imageId);

        } catch (NotFoundException e) {
            log.trace("Image {} does not exist", imageId);
            exists = false;
        }
        return exists;
    }

    public void stopAndRemoveContainer(String containerName) {
        log.debug("Stop and remove container {}", containerName);
        stopContainer(containerName);
        removeContainer(containerName);
    }

    public void stopContainer(String containerName) {
        if (isRunningContainer(containerName)) {
            log.trace("Stopping container {}", containerName);
            dockerClient.stopContainerCmd(containerName).exec();

        } else {
            log.debug("Container {} is not running", containerName);
        }
    }

    public void removeContainer(String containerName) {
        if (existsContainer(containerName)) {
            log.trace("Removing container {}", containerName);
            dockerClient.removeContainerCmd(containerName).withForce(true)
                    .withRemoveVolumes(true).exec();
        }
    }

    public String execCommand(String containerName, boolean awaitCompletion,
            String... command) throws IOException, InterruptedException {
        assert (command.length > 0);

        String output = null;
        String commandStr = Arrays.toString(command);

        log.trace("Executing command {} in container {} (await completion {})",
                commandStr, containerName, awaitCompletion);

        if (existsContainer(containerName)) {
            ExecCreateCmdResponse exec = dockerClient
                    .execCreateCmd(containerName).withCmd(command).withTty(true)
                    .withAttachStdin(true).withAttachStdout(true)
                    .withAttachStderr(true).exec();

            log.trace("Command executed. Exec id: {}", exec.getId());
            OutputStream outputStream = new ByteArrayOutputStream();
            try (ExecStartResultCallback startResultCallback = dockerClient
                    .execStartCmd(exec.getId()).withDetach(false).withTty(true)
                    .exec(new ExecStartResultCallback(outputStream,
                            outputStream))) {

                if (awaitCompletion) {
                    startResultCallback.awaitCompletion();
                }
                output = outputStream.toString();

            } finally {
                log.trace("Callback terminated. Result: {}", output);
            }
        }
        return output;
    }

    public InputStream getFileFromContainer(String containerName,
            String fileName) {
        InputStream inputStream = null;
        if (existsContainer(containerName)) {
            log.trace("Copying {} from container {}", fileName, containerName);

            inputStream = dockerClient
                    .copyArchiveFromContainerCmd(containerName, fileName)
                    .exec();
        }
        return inputStream;
    }

    public void waitForContainer(String containerName)
            throws InterruptedException {
        boolean isRunning = false;
        long timeoutMs = currentTimeMillis()
                + SECONDS.toMillis(dockerWaitTimeoutSec);
        do {
            isRunning = isRunningContainer(containerName);
            if (!isRunning) {
                // Check timeout
                if (currentTimeMillis() > timeoutMs) {
                    throw new DockerException(
                            "Timeout of " + dockerWaitTimeoutSec
                                    + " seconds waiting for container "
                                    + containerName);
                }

                // Wait poll time
                log.trace("Container {} is not still running ... waiting {} ms",
                        containerName, dockerPollTimeMs);
                sleep(dockerPollTimeMs);

            }
        } while (!isRunning);
    }

    public boolean isRunningContainer(String containerName) {
        boolean isRunning = false;
        if (existsContainer(containerName)) {
            isRunning = dockerClient.inspectContainerCmd(containerName).exec()
                    .getState().getRunning();
            log.trace("Container {} is running: {}", containerName, isRunning);
        }

        return isRunning;
    }

    public boolean existsContainer(String containerName) {
        boolean exists = true;
        try {
            log.trace("Checking if container {} exists", containerName);
            dockerClient.inspectContainerCmd(containerName).exec();
            log.trace("Container {} already exist", containerName);

        } catch (NotFoundException e) {
            log.debug("Container {} does not exist", containerName);
            exists = false;
        }
        return exists;
    }

    public void waitForHostIsReachable(String url) {
        long timeoutMillis = MILLISECONDS.convert(dockerWaitTimeoutSec,
                SECONDS);
        long endTimeMillis = System.currentTimeMillis() + timeoutMillis;

        log.debug("Waiting for {} to be reachable (timeout {} seconds)", url,
                dockerWaitTimeoutSec);
        String errorMessage = "URL " + url + " not reachable in "
                + dockerWaitTimeoutSec + " seconds";
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[] {};
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs,
                            String authType) {
                        // No actions required
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs,
                            String authType) {
                        // No actions required
                    }
                } };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            waitUrl(url, timeoutMillis, endTimeMillis, errorMessage);

        } catch (Exception e) {
            // Not propagating multiple exceptions (NoSuchAlgorithmException,
            // KeyManagementException, IOException, InterruptedException) to
            // improve readability
            throw new DockerException(errorMessage, e);
        }

    }

    private void waitUrl(String url, long timeoutMillis, long endTimeMillis,
            String errorMessage) throws IOException, InterruptedException {
        int responseCode = 0;
        while (true) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url)
                        .openConnection();
                connection.setConnectTimeout((int) timeoutMillis);
                connection.setReadTimeout((int) timeoutMillis);
                connection.setRequestMethod("GET");
                responseCode = connection.getResponseCode();

                if (responseCode == HTTP_OK || responseCode == HTTP_NOT_FOUND) {
                    log.debug("URL already reachable");
                    break;
                } else {
                    log.trace(
                            "URL {} not reachable (response {}). Trying again in {} ms",
                            url, responseCode, dockerPollTimeMs);
                }

            } catch (SSLHandshakeException | SocketException e) {
                log.trace("Error {} waiting URL {}, trying again in {} ms",
                        e.getMessage(), url, dockerPollTimeMs);

            } finally {
                // Polling to wait a consistent state
                sleep(dockerPollTimeMs);
            }

            if (currentTimeMillis() > endTimeMillis) {
                throw new DockerException(errorMessage);
            }
        }
    }

    public String generateContainerName(String prefix) {
        return prefix + randomUUID().toString();
    }

    public int findRandomOpenPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public String getContainerIpAddress(String containerName)
            throws IOException {
        String ipAddress;
        if (IS_OS_WINDOWS) {
            ipAddress = getDockerServerIp();
        } else {
            Map<String, ContainerNetwork> networks = dockerClient
                    .inspectContainerCmd(containerName).exec()
                    .getNetworkSettings().getNetworks();
            ipAddress = networks.values().iterator().next().getIpAddress();
        }
        return ipAddress;
    }

}
