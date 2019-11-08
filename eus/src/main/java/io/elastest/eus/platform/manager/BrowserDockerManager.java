package io.elastest.eus.platform.manager;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.HostConfig.Bind;
import com.spotify.docker.client.messages.HostConfig.Bind.Builder;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.model.DockerPullImageProgress;
import io.elastest.epm.client.model.DockerServiceStatus;
import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.service.DockerService;
import io.elastest.epm.client.utils.UtilTools;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.EusContextProperties;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.services.model.WebRTCQoEMeter;
import io.elastest.eus.session.SessionManager;

public class BrowserDockerManager extends PlatformManager {
    final Logger logger = getLogger(lookup().lookupClass());

    private DockerService dockerService;

    public BrowserDockerManager(DockerService dockerService, EusFilesService eusFilesService,
            EusContextProperties contextProperties) {
        super(eusFilesService, contextProperties);
        this.dockerService = dockerService;
    }

    public List<String> getContainerNetworksByContainerPrefix(String prefix) throws Exception {
        List<String> networks = new ArrayList<>();
        List<Container> containers = dockerService.getContainersByNamePrefix(prefix);
        if (containers != null && containers.size() > 0) {
            networks = dockerService.getContainerNetworks(containers.get(0).id());
        }
        return networks;
    }

    @Override
    public void downloadFileOrFilesFromServiceToEus(String instanceId, String remotePath,
            String localPath, String originalFilename, String newFilename, Boolean isDirectory)
            throws Exception {
        downloadFileOrFilesFromSubServiceToEus(instanceId, instanceId, remotePath, localPath,
                originalFilename, newFilename, isDirectory);
    }

    @Override
    public void downloadFileOrFilesFromSubServiceToEus(String instanceId, String subServiceID,
            String remotePath, String localPath, String originalFilename, String newFilename,
            Boolean isDirectory) throws Exception {
        if (isDirectory) {
            InputStream fileInputStream = getFileFromService(instanceId, remotePath, isDirectory);
            // TODO
        } else {
            String completeFilePath = remotePath.endsWith(EusFilesService.FILE_SEPARATOR)
                    ? remotePath
                    : remotePath + EusFilesService.FILE_SEPARATOR;
            completeFilePath += originalFilename;
            InputStream fileInputStream = getFileFromService(instanceId, completeFilePath,
                    isDirectory);

            final byte[] fileByteArray = IOUtils.toByteArray(fileInputStream);

            eusFilesService.saveByteArrayFileToPathInEUS(localPath, newFilename, fileByteArray);
        }

    }

    @Override
    public InputStream getFileFromService(String serviceNameOrId, String path, Boolean isDirectory)
            throws Exception {
        // Note!!!: if file does not exists, spotify docker
        // returns ContainernotFoundException (bug)
        if (isDirectory) {
            return dockerService.getFilesFromContainerAsInputStreamTar(serviceNameOrId, path);
        } else {
            return dockerService.getSingleFileFromContainer(serviceNameOrId, path);
        }
    }

    @Override
    public InputStream getFileFromSubService(String instanceId, String subServiceID, String path,
            Boolean isDirectory) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSessionContextInfo(SessionManager sessionManager) throws Exception {
        String vncContainerName = sessionManager.getVncContainerName();
        if (vncContainerName != null) {
            return dockerService.getContainerInfoStringByName(vncContainerName);
        }
        return "";
    }

    @Override
    public void buildAndRunBrowserInContainer(SessionManager sessionManager, String containerPrefix,
            String originalRequestBody, String folderPath, ExecutionData execData,
            List<String> envs, Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception {
        String hubContainerName = generateRandomContainerNameWithPrefix(containerPrefix, execData);

        /* **** Volumes **** */
        logger.info("Folder path in host: {}", folderPath);
        List<Bind> volumes = new ArrayList<>();

        // Recording
        Builder recordingsVolumeBuilder = Bind.builder();
        recordingsVolumeBuilder.from(folderPath);
        recordingsVolumeBuilder.to(contextProperties.CONTAINER_RECORDING_FOLDER);
        volumes.add(recordingsVolumeBuilder.build());

        // Shared files
        Builder sharedfilesVolumeBuilder = Bind.builder();
        String hostSharedFilesFolderPath = folderPath
                + (folderPath.endsWith(EusFilesService.FILE_SEPARATOR) ? ""
                        : EusFilesService.FILE_SEPARATOR)
                + contextProperties.HOST_SHARED_FILES_RELATIVE_FOLDER;

        eusFilesService.createFolderIfNotExists(hostSharedFilesFolderPath);

        sharedfilesVolumeBuilder.from(hostSharedFilesFolderPath);
        sharedfilesVolumeBuilder.to(contextProperties.CONTAINER_SHARED_FILES_FOLDER);
        volumes.add(sharedfilesVolumeBuilder.build());

        /* **** Port binding **** */
        Map<String, List<PortBinding>> portBindings = new HashMap<>();

        int hubPort = dockerService.findRandomOpenPort();
        String exposedHubPort = Integer.toString(contextProperties.HUB_EXPOSED_PORT);
        portBindings.put(exposedHubPort, Arrays.asList(PortBinding.of("0.0.0.0", hubPort)));

        int vncPort = dockerService.findRandomOpenPort();
        String exposedVncPort = Integer.toString(contextProperties.HUB_VNC_EXPOSED_PORT);
        portBindings.put(exposedVncPort, Arrays.asList(PortBinding.of("0.0.0.0", vncPort)));

        int noVncBindedPort = dockerService.findRandomOpenPort();
        String exposedNoVncPort = Integer.toString(contextProperties.NO_VNC_EXPOSED_PORT);
        portBindings.put(exposedNoVncPort,
                Arrays.asList(PortBinding.of("0.0.0.0", noVncBindedPort)));

        /* **** Exposed ports **** */
        List<String> exposedPorts = asList(exposedHubPort, exposedVncPort, exposedNoVncPort);

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(imageId);
        dockerBuilder.containerName(hubContainerName);
        dockerBuilder.exposedPorts(exposedPorts);
        dockerBuilder.portBindings(portBindings);
        dockerBuilder.volumeBindList(volumes);
        dockerBuilder.shmSize(contextProperties.SHM_SIZE);
        dockerBuilder.envs(envs);
        dockerBuilder.capAdd(asList("SYS_ADMIN"));
        dockerBuilder.labels(labels);

        if (capabilities.getExtraHosts() != null) {
            dockerBuilder.extraHosts(capabilities.getExtraHosts());
        }

        /* **** Obtain networks for the browser **** */
        Map<String, List<String>> networksMap = getNetworksFromExecutionData(execData);

        List<String> networks = networksMap.get("networks");
        String network = networksMap.get("network").get(0);

        if (contextProperties.USE_TORM) {
            dockerBuilder.network(network);
        }
        /* **** Save info **** */
        sessionManager.setHubContainerName(hubContainerName);
        sessionManager.setVncContainerName(hubContainerName);
        sessionManager.setStatus(DockerServiceStatusEnum.INITIALIZING);
        sessionManager.setStatusMsg("Initializing...");

        /* **** Pull **** */
        dockerService.pullImageWithProgressHandler(imageId,
                getBrowserProgressHandler(imageId, sessionManager));
        sessionManager.setStatus(DockerServiceStatusEnum.STARTING);
        sessionManager.setStatusMsg("Starting...");

        /* **** Start **** */
        String containerId = dockerService.createAndStartContainerWithPull(dockerBuilder.build(),
                true);

        /* **** Additional Networks **** */
        if (networks != null && networks.size() > 0) {
            logger.debug("Inserting browser container into additional networks");
            for (String additionalNetwork : networks) {
                if (additionalNetwork != null && !"".equals(additionalNetwork)) {
                    logger.debug("Inserting browser container into {} network", additionalNetwork);
                    dockerService.insertIntoNetwork(additionalNetwork, containerId);
                }
            }
        }

        /* **** Set IPs and ports **** */
        sessionManager.setHubIp(dockerService.getDockerServerIp());
        sessionManager.setHubPort(hubPort);
        sessionManager.setNoVncBindedPort(noVncBindedPort);
    }

    @Override
    public BrowserSync buildAndRunBrowsersyncService(SessionManager sessionManager,
            ExecutionData execData, CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception {
        String serviceContainerName = getBrowserSyncServiceName(execData);
        BrowserSync browsersync = new BrowserSync(crossBrowserCapabilities);

        DesiredCapabilities desiredCapabilities = crossBrowserCapabilities.getDesiredCapabilities();

        String sutUrl = crossBrowserCapabilities.getSutUrl();

        List<String> envs = new ArrayList<>();
        String optionsEnv = "BROWSER_SYNC_OPTIONS=--proxy '" + sutUrl + "' --open 'external'";
        envs.add(optionsEnv);

        /* **** Obtain networks **** */
        Map<String, List<String>> networksMap = getNetworksFromExecutionData(execData);

        List<String> networks = networksMap.get("networks");
        String network = networksMap.get("network").get(0);

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(
                contextProperties.EUS_SERVICE_BROWSERSYNC_IMAGE_NAME);
        dockerBuilder.containerName(serviceContainerName);
        dockerBuilder.envs(envs);
        dockerBuilder.labels(labels);
        dockerBuilder.network(network);

        // ExtraHosts
        if (desiredCapabilities.getExtraHosts() != null) {
            dockerBuilder.extraHosts(desiredCapabilities.getExtraHosts());
        }

        /* **** Start **** */
        String containerId = dockerService.createAndStartContainerWithPull(dockerBuilder.build(),
                true);

        /* **** Additional Networks **** */
        if (networks != null && networks.size() > 0) {
            logger.debug("Inserting Browsersync service container into additional networks");
            for (String additionalNetwork : networks) {
                if (additionalNetwork != null && !"".equals(additionalNetwork)) {
                    logger.debug("Inserting Browsersync service container into {} network",
                            additionalNetwork);
                    dockerService.insertIntoNetwork(additionalNetwork, containerId);
                }
            }
        }

        String ip = dockerService.getContainerIp(containerId, network);
        String guiUrl = "http://" + ip + ":" + contextProperties.EUS_SERVICE_BROWSERSYNC_GUI_PORT;

        URL sutUrlObj = new URL(sutUrl);
        String appProtocol = sutUrlObj.getProtocol();
        String appUrl = appProtocol + "://" + ip + ":"
                + contextProperties.EUS_SERVICE_BROWSERSYNC_APP_PORT;

        if (sutUrlObj.getFile() != null) {
            appUrl += sutUrlObj.getFile();
        }

        browsersync.setIdentifier(serviceContainerName);
        browsersync.setGuiUrl(guiUrl);
        browsersync.setAppUrl(appUrl);

        return browsersync;
    }

    @Override
    public WebRTCQoEMeter buildAndRunWebRTCQoEMeterService(SessionManager sessionManager,
            ExecutionData execData, Map<String, String> labels) throws Exception {
        String serviceContainerName = getWebRTCQoEMeterServiceName(execData);
        WebRTCQoEMeter webRTCQoEMeter = new WebRTCQoEMeter();

        /* **** Obtain networks **** */
        Map<String, List<String>> networksMap = getNetworksFromExecutionData(execData);

        List<String> networks = networksMap.get("networks");
        String network = networksMap.get("network").get(0);

        /* **** Volumes **** */
        List<Bind> volumes = new ArrayList<>();

        // Shared files
        String folderPath = eusFilesService.getSessionFilesFolderBySessionManager(sessionManager);

        Builder sharedfilesVolumeBuilder = Bind.builder();
        String hostSharedFilesFolderPath = folderPath
                + (folderPath.endsWith(EusFilesService.FILE_SEPARATOR) ? ""
                        : EusFilesService.FILE_SEPARATOR)
                + contextProperties.HOST_SHARED_FILES_RELATIVE_FOLDER;

        eusFilesService.createFolderIfNotExists(hostSharedFilesFolderPath);

        sharedfilesVolumeBuilder.from(hostSharedFilesFolderPath);
        sharedfilesVolumeBuilder.to(contextProperties.CONTAINER_SHARED_FILES_FOLDER);
        volumes.add(sharedfilesVolumeBuilder.build());

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(
                contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_NAME);
        dockerBuilder.containerName(serviceContainerName);
        dockerBuilder.labels(labels);
        dockerBuilder.network(network);
        dockerBuilder.volumeBindList(volumes);
        dockerBuilder.cmd(Arrays
                .asList(contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_COMMAND.split("\\s")));

        /* **** Start **** */
        String containerId = dockerService.createAndStartContainerWithPull(dockerBuilder.build(),
                true);

        /* **** Additional Networks **** */
        if (networks != null && networks.size() > 0) {
            logger.debug("Inserting WebRTCQoEMeter service container into additional networks");
            for (String additionalNetwork : networks) {
                if (additionalNetwork != null && !"".equals(additionalNetwork)) {
                    logger.debug("Inserting WebRTCQoEMeter service container into {} network",
                            additionalNetwork);
                    dockerService.insertIntoNetwork(additionalNetwork, containerId);
                }
            }
        }

        webRTCQoEMeter.setIdentifier(serviceContainerName);

        return webRTCQoEMeter;
    }

    public Map<String, List<String>> getNetworksFromExecutionData(ExecutionData execData)
            throws Exception {
        Map<String, List<String>> networksMap = new HashMap<>();
        String network = contextProperties.DOCKER_NETWORK;
        List<String> networks = new ArrayList<>();

        if (execData != null) {
            String sutPrefix = execData.getSutContainerPrefix();

            List<String> additionalNetworks = new ArrayList<>();
            if (execData.isUseSutNetwork() && sutPrefix != null && !"".equals(sutPrefix)) {
                logger.debug("Sut prefix: {}", sutPrefix);

                additionalNetworks = getContainerNetworksByContainerPrefix(sutPrefix);
                if (additionalNetworks.size() > 0) {
                    logger.debug("Sut networks: {}", additionalNetworks);

                    if (additionalNetworks != null) {
                        network = additionalNetworks.get(0);
                        boolean first = true;
                        for (String currentNetwork : additionalNetworks) {
                            if (!first && currentNetwork != null) {
                                networks.add(currentNetwork);
                            }
                            first = false;
                        }
                    }
                    if (additionalNetworks == null || additionalNetworks.size() == 0
                            || network == null || "".equals(network)) {
                        network = contextProperties.DOCKER_NETWORK;
                        logger.error(
                                "Error on get Sut network to use with External TJob. Using default ElasTest network  {}",
                                contextProperties.DOCKER_NETWORK);
                    }
                    logger.debug("First Sut network: {}", network);
                    logger.debug("Sut additional networks: {}", networks);
                }
            }
        }
        networksMap.put("network", Arrays.asList(network));
        networksMap.put("networks", networks);

        return networksMap;
    }

    public void waitForBrowserReady(String internalVncUrl, SessionManager sessionManager)
            throws Exception {
        try {
            UtilTools.waitForHostIsReachable(internalVncUrl, 20);
            sessionManager.setStatusMsg("Ready");
            sessionManager.setStatus(DockerServiceStatusEnum.READY);
        } catch (Exception e) {
            logger.error("Error on wait for host reachable: {}", e.getMessage());
            removeServiceWithTimeout(sessionManager.getHubContainerName(), 60);
            throw e;
        }
    }

    private ProgressHandler getBrowserProgressHandler(String image,
            DockerServiceStatus dockerServiceStatus) {
        DockerPullImageProgress dockerPullImageProgress = new DockerPullImageProgress();
        dockerPullImageProgress.setImage(image);
        dockerPullImageProgress.setCurrentPercentage(0);

        dockerServiceStatus.setStatus(DockerServiceStatusEnum.PULLING);
        dockerServiceStatus.setStatusMsg("Pulling " + image + " image");
        return new ProgressHandler() {
            @Override
            public void progress(ProgressMessage message) throws DockerException {
                dockerPullImageProgress.processNewMessage(message);
                String msg = "Pulling image " + image + ": "
                        + dockerPullImageProgress.getCurrentPercentage() + "%";

                dockerServiceStatus.setStatusMsg(msg);
            }

        };

    }

    @Override
    public String execCommandInBrowser(String hubContainerName, boolean awaitCompletion,
            String... command) throws Exception {
        return dockerService.execCommand(hubContainerName, awaitCompletion, command);
    }

    @Override
    public String execCommand(String dockerContainerIdOrName, String command) throws Exception {
        return execCommandInBrowser(dockerContainerIdOrName, true, "sh", "-c", command);
    }

    @Override
    public String execCommandInSubService(String instanceId, String subserviceId,
            boolean awaitCompletion, String command) throws Exception {
        return execCommand(subserviceId, command);
    }

    @Override
    public boolean existServiceWithName(String name) throws Exception {
        return dockerService.existsContainer(name);
    }

    @Override
    public void removeServiceWithTimeout(String containerId, int killAfterSeconds)
            throws Exception {
        dockerService.stopAndRemoveContainerWithKillTimeout(containerId, killAfterSeconds);
    }

    @Override
    public void copyFilesFromBrowserIfNecessary(SessionManager sessionManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void uploadFile(SessionManager sessionManager, String serviceNameOrId,
            InputStream tarStreamFile, String completeFilePathWithoutName, String fileName)
            throws Exception {
        execCommandInBrowser(serviceNameOrId, true, "sudo", "mkdir", "-p",
                completeFilePathWithoutName);

        dockerService.copyFileToContainer(serviceNameOrId, tarStreamFile,
                completeFilePathWithoutName);
    }

    @Override
    public void uploadFileToSubservice(SessionManager sessionManager, String instanceId,
            String subServiceID, InputStream tarStreamFile, String completeFilePathWithoutName,
            String fileName) throws Exception {
        uploadFile(sessionManager, subServiceID, tarStreamFile, completeFilePathWithoutName,
                fileName);
    }

    @Override
    public void uploadFileFromEus(SessionManager sessionManager, String serviceNameOrId,
            String filePathInEus, String fileNameInEus, String targetFilePath,
            String targetFileName) throws Exception {
        boolean isDirectory = true;

        // Create path in container first
        execCommandInBrowser(serviceNameOrId, true, "sudo", "mkdir", "-p", targetFilePath);

        String completeFilePathInEUS = (filePathInEus.endsWith(EusFilesService.FILE_SEPARATOR)
                ? filePathInEus
                : filePathInEus + EusFilesService.FILE_SEPARATOR);

        String completeTargetPath = targetFilePath.endsWith(EusFilesService.FILE_SEPARATOR)
                ? targetFilePath
                : targetFilePath + EusFilesService.FILE_SEPARATOR;

        if (fileNameInEus != null) {
            completeFilePathInEUS += fileNameInEus;
            isDirectory = false;

            if (targetFileName == null) {
                targetFileName = fileNameInEus;
            }
            // completeTargetPath += targetFileName;
        }
        //
        // Path fromPath = Paths.get(completeFilePathInEUS);
        // dockerService.copyFileToContainer(serviceNameOrId, fromPath,
        // completeTargetPath);

        // Upload file to et shared files folder (copying directly to eus
        // volume folder)
        String eusSharedFilesPath = eusFilesService.getEusSharedFilesPath(sessionManager);

        // ~/.elastest/eus/sessionId/ || ~/.elastest/tjobs/.../eus/sessionId/
        String sessionEusSharedFilesPath = eusSharedFilesPath + sessionManager.getSessionId()
                + EusFilesService.FILE_SEPARATOR;

        String sessionInternalSharedFilesPath = eusFilesService.getInternalSharedFilesPath(
                sessionManager) + sessionManager.getSessionId() + EusFilesService.FILE_SEPARATOR;

        if (isDirectory) {
            File tmpFolderInEus = new File(completeFilePathInEUS);

            Files.move(tmpFolderInEus.toPath(),
                    new File(sessionEusSharedFilesPath + tmpFolderInEus.getName()).toPath());

            // Copy from shared folder to target folder into container
            execCommandInBrowser(serviceNameOrId, true, "sudo", "cp", "-R",
                    sessionInternalSharedFilesPath + tmpFolderInEus.getName(),
                    completeTargetPath + tmpFolderInEus.getName());

        } else {
            // Move to shared folder with volume first
            File tmpFileInEus = new File(completeFilePathInEUS);
            Files.move(tmpFileInEus.toPath(),
                    new File(sessionEusSharedFilesPath + targetFileName).toPath());

            // Copy from shared folder to target folder into container
            execCommandInBrowser(serviceNameOrId, true, "sudo", "cp",
                    sessionInternalSharedFilesPath + targetFileName,
                    completeTargetPath + targetFileName);
        }

    }

    @Override
    // instanceId and subServiceID are the same in this case
    public void uploadFileToSubserviceFromEus(SessionManager sessionManager, String instanceId,
            String subServiceID, String filePathInEus, String fileNameInEus, String targetFilePath,
            String targetFileName) throws Exception {
        uploadFileFromEus(sessionManager, subServiceID, filePathInEus, fileNameInEus,
                targetFilePath, targetFileName);
    }

    @Override
    public Boolean uploadFileToBrowser(SessionManager sessionManager, ExecutionData execData,
            MultipartFile file, String path) throws Exception {
        // If not path, upload file to et shared files folder (copying directly
        // to eus volume folder)
        if (path == null || "".equals(path)) {
            path = eusFilesService.getEusSharedFilesPath(sessionManager);
            return eusFilesService.saveFileToPathInEUS(path, file.getOriginalFilename(), file);
        } else {
            String targetPath = path.endsWith(file.getOriginalFilename())
                    ? eusFilesService.getPathWithoutFileNameFromCompleteFilePath(path)
                    : path;

            uploadFile(sessionManager, sessionManager.getVncContainerName(), file.getInputStream(),
                    targetPath, file.getOriginalFilename());
            return true;
        }
    }

    @Override
    public Boolean uploadFileFromUrlToBrowser(SessionManager sessionManager, ExecutionData execData,
            String fileUrl, String completeFilePath, String fileName) throws Exception {
        // Upload file to et shared files folder (copying directly to eus volume
        // folder)
        String eusSharedFilesPath = eusFilesService.getEusSharedFilesPath(sessionManager);
        // ~/.elastest/eus/sessionId/ || ~/.elastest/tjobs/.../eus/sessionId/
        String sessionEusSharedFilesPath = eusSharedFilesPath + sessionManager.getSessionId()
                + EusFilesService.FILE_SEPARATOR;
        // shared with volume
        eusFilesService.saveFileFromUrlToPathInEUS(sessionEusSharedFilesPath, fileName, fileUrl);

        // if completeFilePath is not empty, move file to folder in container
        if (completeFilePath != null && !"".equals(completeFilePath)) {
            completeFilePath = completeFilePath.endsWith(EusFilesService.FILE_SEPARATOR)
                    ? completeFilePath
                    : completeFilePath + EusFilesService.FILE_SEPARATOR;

            // Create path in container first
            execCommandInBrowser(sessionManager.getVncContainerName(), true, "sudo", "mkdir", "-p",
                    completeFilePath);

            String internalSharedFilesPath = eusFilesService
                    .getInternalSharedFilesPath(sessionManager);

            execCommandInBrowser(sessionManager.getVncContainerName(), true, "sudo", "cp",
                    internalSharedFilesPath + sessionManager.getSessionId()
                            + EusFilesService.FILE_SEPARATOR + fileName,
                    completeFilePath + fileName);
        }
        return true;
    }

    @Override
    public List<String> getFolderFilesList(String containerId, String remotePath, String filter)
            throws Exception {
        return dockerService.getFilesListFromContainerFolder(containerId, remotePath, filter);
    }

    @Override
    public List<String> getSubserviceFolderFilesList(String instanceId, String subServiceId,
            String remotePath, String filter) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
