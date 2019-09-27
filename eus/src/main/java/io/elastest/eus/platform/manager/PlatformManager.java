package io.elastest.eus.platform.manager;

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

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.EusContextProperties;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.services.model.WebRTCQoEMeter;
import io.elastest.eus.session.SessionManager;

public abstract class PlatformManager {
    final Logger logger = getLogger(lookup().lookupClass());

    protected EusContextProperties contextProperties;
    protected EusFilesService eusFilesService;

    public PlatformManager(EusFilesService eusFilesService,
            EusContextProperties contextProperties) {
        super();
        this.eusFilesService = eusFilesService;
        this.contextProperties = contextProperties;
    }

    public abstract InputStream getFileFromBrowser(
            SessionManager sessionManager, String path, Boolean isDirectory)
            throws Exception;

    public abstract void copyFilesFromBrowserIfNecessary(
            SessionManager sessionManager) throws IOException;

    public abstract String getSessionContextInfo(SessionManager sessionManager)
            throws Exception;

    public String generateRandomContainerNameWithPrefix(String prefix,
            ExecutionData execData) {
        String name = prefix + randomUUID().toString();

        if (execData != null && execData.gettJobExecId() != null) {
            name += "-exec-" + execData.gettJobExecId();
        }

        return name;
    }

    public abstract void buildAndRunBrowserInContainer(
            SessionManager sessionManager, String containerPrefix,
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
            SessionManager sessionManager) throws Exception;

    public abstract BrowserSync buildAndRunBrowsersyncService(
            ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception;

    public abstract void uploadFile(String serviceNameOrId,
            InputStream tarStreamFile, String completePresenterPath)
            throws Exception;

    public abstract List<String> getFolderFilesList(String containerId,
            String remotePath, String filter) throws Exception;

    public abstract WebRTCQoEMeter buildAndRunWebRTCQoEMeterService(
            ExecutionData execData, Map<String, String> labels)
            throws Exception;

    /* *************************************** */
    /* ********* Implemented Methods ********* */
    /* *************************************** */

    @SuppressWarnings("static-access")
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

    public String getBrowserSyncServiceName(ExecutionData execData) {
        return generateRandomContainerNameWithPrefix(
                contextProperties.EUS_CONTAINER_PREFIX
                        + contextProperties.EUS_SERVICE_BROWSERSYNC_PREFIX,
                execData);
    }

    public String getWebRTCQoEMeterServiceName(ExecutionData execData) {
        return generateRandomContainerNameWithPrefix(
                contextProperties.EUS_CONTAINER_PREFIX
                        + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_PREFIX,
                execData);
    }

    @Override
    public String toString() {
        return "";
    }

}
