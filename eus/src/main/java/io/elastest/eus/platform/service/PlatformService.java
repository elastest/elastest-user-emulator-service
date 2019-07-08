package io.elastest.eus.platform.service;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;

public abstract class PlatformService {
    final Logger logger = getLogger(lookup().lookupClass());

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

    @Value("${host.shared.files.relative.folder}")
    protected String hostSharedFilesRelativeFolder;
    @Value("${container.recording.folder}")
    protected String containerRecordingFolder;
    @Value("${container.recording.folder}")
    protected String containerSharedFilesFolder;

    @Autowired
    private EusFilesService eusFilesService;

    public abstract List<String> getContainerNetworksByContainerPrefix(
            String prefix) throws Exception;

    public abstract InputStream getFileFromBrowser(
            DockerBrowserInfo dockerBrowserInfo, String path,
            Boolean isDirectory) throws Exception;

    public abstract void copyFilesFromBrowserIfNecessary(
            DockerBrowserInfo dockerBrowserInfo, String instanceId) throws IOException;

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

    public abstract void waitForBrowserReady(String serviceNameOrId,
            String internalVncUrl, DockerBrowserInfo dockerBrowserInfo)
            throws Exception;

    public abstract BrowserSync buildAndRunBrowsersyncService(
            ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception;

    protected String createRecordingsPath(String hostPath) {
        logger.debug("Creating recordings path from: {}", hostPath);
        String recordingsPath = "";
        String pathRecordingsInHost = hostPath
                + (hostPath.endsWith(EusFilesService.FILE_SEPARATOR) ? ""
                        : EusFilesService.FILE_SEPARATOR);
        String recordingsRelativePath = pathRecordingsInHost
                .substring(
                        pathRecordingsInHost
                                .indexOf(eusFilesService.FILE_SEPARATOR,
                                        pathRecordingsInHost.indexOf(
                                                eusFilesService.FILE_SEPARATOR)
                                                + 1));
        recordingsPath = eusFilesService.getEtSharedFolder()
                + recordingsRelativePath;

        return recordingsPath;
    }

    protected void moveFiles(File fileToMove, String targetPath)
            throws IOException {
        if (fileToMove.isDirectory()) {
            for (File file : fileToMove.listFiles()) {
                moveFiles(file, targetPath + "/" + file.getName());
            }
        } else {
            try {
                Files.move(Paths.get(fileToMove.getPath()),
                        Paths.get(targetPath),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Error moving files to other directory.");
                throw e;
            }
        }

    }
}
