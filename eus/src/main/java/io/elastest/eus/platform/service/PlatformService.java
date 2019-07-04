package io.elastest.eus.platform.service;

import static java.util.UUID.randomUUID;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.services.model.BrowserSync;

public abstract class PlatformService {

    @Value("${hub.exposedport}")
    protected int hubExposedPort;
    @Value("${hub.vnc.exposedport}")
    protected int hubVncExposedPort;
    @Value("${hub.novnc.exposedport}")
    protected int noVncExposedPort;
    @Value("${browser.shm.size}")
    protected long shmSize;

    @Value("${eus.container.prefix}")
    protected String eusContainerPrefix;

    @Value("${eus.service.browsersync.prefix}")
    protected String eusServiceBrowsersyncPrefix;

    @Value("${eus.service.browsersync.image.name}")
    protected String eusServiceBrowsersyncImageName;

    @Value("${eus.service.browsersync.gui.port}")
    protected String eusServiceBrowsersyncGUIPort;

    @Value("${eus.service.browsersync.app.port}")
    protected String eusServiceBrowsersyncAppPort;

    public abstract List<String> getContainerNetworksByContainerPrefix(
            String prefix) throws Exception;

    public abstract InputStream getFileFromBrowser(
            DockerBrowserInfo dockerBrowserInfo, String path,
            Boolean isDirectory) throws Exception;

    public abstract void copyFilesFromBrowserIfNecessary(
            DockerBrowserInfo dockerBrowserInfo) throws IOException;

    public abstract String getSessionContextInfo(
            DockerBrowserInfo dockerBrowserInfo) throws Exception;

    public String generateRandomContainerNameWithPrefix(String prefix) {
        return prefix + randomUUID().toString();
    }

    public abstract void buildAndRunBrowserInContainer(
            DockerBrowserInfo dockerBrowserInfo, String containerPrefix,
            String originalRequestBody, String folderPath,
            ExecutionData execData, List<String> envs,
            Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception;

    public abstract void execCommand(String hubContainerName,
            boolean awaitCompletion, String... command) throws Exception;

    public abstract boolean existServiceWithName(String name) throws Exception;

    public abstract void removeServiceWithTimeout(String containerId,
            int killAfterSeconds) throws Exception;

    public abstract void waitForBrowserReady(String internalVncUrl,
            DockerBrowserInfo dockerBrowserInfo) throws Exception;

    public abstract BrowserSync buildAndRunBrowsersyncService(
            ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception;
}
