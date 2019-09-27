package io.elastest.eus.platform.manager;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

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

    public BrowserDockerManager(DockerService dockerService,
            EusFilesService eusFilesService,
            EusContextProperties contextProperties) {
        super(eusFilesService, contextProperties);
        this.dockerService = dockerService;
    }

    public List<String> getContainerNetworksByContainerPrefix(String prefix)
            throws Exception {
        List<String> networks = new ArrayList<>();
        List<Container> containers = dockerService
                .getContainersByNamePrefix(prefix);
        if (containers != null && containers.size() > 0) {
            networks = dockerService
                    .getContainerNetworks(containers.get(0).id());
        }
        return networks;
    }

    @Override
    public InputStream getFileFromBrowser(SessionManager sessionManager,
            String path, Boolean isDirectory) throws Exception {
        // Note!!!: if file does not exists, spotify docker
        // returns ContainernotFoundException (bug)

        if (isDirectory) {
            return dockerService.getFilesFromContainerAsInputStreamTar(
                    sessionManager.getVncContainerName(), path);
        } else {
            return dockerService.getSingleFileFromContainer(
                    sessionManager.getVncContainerName(), path);
        }
    }

    @Override
    public String getSessionContextInfo(SessionManager sessionManager)
            throws Exception {
        String vncContainerName = sessionManager.getVncContainerName();
        if (vncContainerName != null) {
            return dockerService.getContainerInfoStringByName(vncContainerName);
        }
        return "";
    }

    @Override
    public void buildAndRunBrowserInContainer(SessionManager sessionManager,
            String containerPrefix, String originalRequestBody,
            String folderPath, ExecutionData execData, List<String> envs,
            Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception {
        String hubContainerName = generateRandomContainerNameWithPrefix(
                containerPrefix, execData);

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
        sharedfilesVolumeBuilder
                .to(contextProperties.CONTAINER_SHARED_FILES_FOLDER);
        volumes.add(sharedfilesVolumeBuilder.build());

        /* **** Port binding **** */
        Map<String, List<PortBinding>> portBindings = new HashMap<>();

        int hubPort = dockerService.findRandomOpenPort();
        String exposedHubPort = Integer
                .toString(contextProperties.HUB_EXPOSED_PORT);
        portBindings.put(exposedHubPort,
                Arrays.asList(PortBinding.of("0.0.0.0", hubPort)));

        int vncPort = dockerService.findRandomOpenPort();
        String exposedVncPort = Integer
                .toString(contextProperties.HUB_VNC_EXPOSED_PORT);
        portBindings.put(exposedVncPort,
                Arrays.asList(PortBinding.of("0.0.0.0", vncPort)));

        int noVncBindedPort = dockerService.findRandomOpenPort();
        String exposedNoVncPort = Integer
                .toString(contextProperties.NO_VNC_EXPOSED_PORT);
        portBindings.put(exposedNoVncPort,
                Arrays.asList(PortBinding.of("0.0.0.0", noVncBindedPort)));

        /* **** Exposed ports **** */
        List<String> exposedPorts = asList(exposedHubPort, exposedVncPort,
                exposedNoVncPort);

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
        Map<String, List<String>> networksMap = getNetworksFromExecutionData(
                execData);

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
        String containerId = dockerService
                .createAndStartContainerWithPull(dockerBuilder.build(), true);

        /* **** Additional Networks **** */
        if (networks != null && networks.size() > 0) {
            logger.debug(
                    "Inserting browser container into additional networks");
            for (String additionalNetwork : networks) {
                if (additionalNetwork != null
                        && !"".equals(additionalNetwork)) {
                    logger.debug("Inserting browser container into {} network",
                            additionalNetwork);
                    dockerService.insertIntoNetwork(additionalNetwork,
                            containerId);
                }
            }
        }

        /* **** Set IPs and ports **** */
        sessionManager.setHubIp(dockerService.getDockerServerIp());
        sessionManager.setHubPort(hubPort);
        sessionManager.setNoVncBindedPort(noVncBindedPort);
    }

    public Map<String, List<String>> getNetworksFromExecutionData(
            ExecutionData execData) throws Exception {
        Map<String, List<String>> networksMap = new HashMap<>();
        String network = contextProperties.DOCKER_NETWORK;
        List<String> networks = new ArrayList<>();

        if (execData != null) {
            String sutPrefix = execData.getSutContainerPrefix();

            List<String> additionalNetworks = new ArrayList<>();
            if (execData.isUseSutNetwork() && sutPrefix != null
                    && !"".equals(sutPrefix)) {
                logger.debug("Sut prefix: {}", sutPrefix);

                additionalNetworks = getContainerNetworksByContainerPrefix(
                        sutPrefix);
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
                    if (additionalNetworks == null
                            || additionalNetworks.size() == 0 || network == null
                            || "".equals(network)) {
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

    public void waitForBrowserReady(String internalVncUrl,
            SessionManager sessionManager) throws Exception {
        try {
            UtilTools.waitForHostIsReachable(internalVncUrl, 20);
            sessionManager.setStatusMsg("Ready");
            sessionManager.setStatus(DockerServiceStatusEnum.READY);
        } catch (Exception e) {
            logger.error("Error on wait for host reachable: {}",
                    e.getMessage());
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
            public void progress(ProgressMessage message)
                    throws DockerException {
                dockerPullImageProgress.processNewMessage(message);
                String msg = "Pulling image " + image + ": "
                        + dockerPullImageProgress.getCurrentPercentage() + "%";

                dockerServiceStatus.setStatusMsg(msg);
            }

        };

    }

    public void execCommand(String hubContainerName, boolean awaitCompletion,
            String... command) throws Exception {
        dockerService.execCommand(hubContainerName, awaitCompletion, command);
    }

    @Override
    public boolean existServiceWithName(String name) throws Exception {
        return dockerService.existsContainer(name);
    }

    @Override
    public void removeServiceWithTimeout(String containerId,
            int killAfterSeconds) throws Exception {
        dockerService.stopAndRemoveContainerWithKillTimeout(containerId,
                killAfterSeconds);
    }

    @Override
    public void copyFilesFromBrowserIfNecessary(SessionManager sessionManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public BrowserSync buildAndRunBrowsersyncService(ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception {
        String serviceContainerName = getBrowserSyncServiceName(execData);
        BrowserSync browsersync = new BrowserSync(crossBrowserCapabilities);

        DesiredCapabilities desiredCapabilities = crossBrowserCapabilities
                .getDesiredCapabilities();

        String sutUrl = crossBrowserCapabilities.getSutUrl();

        List<String> envs = new ArrayList<>();
        String optionsEnv = "BROWSER_SYNC_OPTIONS=--proxy '" + sutUrl
                + "' --open 'external'";
        envs.add(optionsEnv);

        /* **** Obtain networks **** */
        Map<String, List<String>> networksMap = getNetworksFromExecutionData(
                execData);

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
        String containerId = dockerService
                .createAndStartContainerWithPull(dockerBuilder.build(), true);

        /* **** Additional Networks **** */
        if (networks != null && networks.size() > 0) {
            logger.debug(
                    "Inserting Browsersync service container into additional networks");
            for (String additionalNetwork : networks) {
                if (additionalNetwork != null
                        && !"".equals(additionalNetwork)) {
                    logger.debug(
                            "Inserting Browsersync service container into {} network",
                            additionalNetwork);
                    dockerService.insertIntoNetwork(additionalNetwork,
                            containerId);
                }
            }
        }

        String ip = dockerService.getContainerIp(containerId, network);
        String guiUrl = "http://" + ip + ":"
                + contextProperties.EUS_SERVICE_BROWSERSYNC_GUI_PORT;

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
    public WebRTCQoEMeter buildAndRunWebRTCQoEMeterService(
            ExecutionData execData, Map<String, String> labels)
            throws Exception {

        String serviceContainerName = getWebRTCQoEMeterServiceName(execData);
        WebRTCQoEMeter webRTCQoEMeter = new WebRTCQoEMeter();

        /* **** Obtain networks **** */
        Map<String, List<String>> networksMap = getNetworksFromExecutionData(
                execData);

        List<String> networks = networksMap.get("networks");
        String network = networksMap.get("network").get(0);

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(
                contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_NAME);
        dockerBuilder.containerName(serviceContainerName);
        dockerBuilder.labels(labels);
        dockerBuilder.network(network);

        /* **** Start **** */
        String containerId = dockerService
                .createAndStartContainerWithPull(dockerBuilder.build(), true);

        /* **** Additional Networks **** */
        if (networks != null && networks.size() > 0) {
            logger.debug(
                    "Inserting WebRTCQoEMeter service container into additional networks");
            for (String additionalNetwork : networks) {
                if (additionalNetwork != null
                        && !"".equals(additionalNetwork)) {
                    logger.debug(
                            "Inserting WebRTCQoEMeter service container into {} network",
                            additionalNetwork);
                    dockerService.insertIntoNetwork(additionalNetwork,
                            containerId);
                }
            }
        }

        webRTCQoEMeter.setIdentifier(serviceContainerName);

        return webRTCQoEMeter;
    }

    @Override
    public void uploadFile(String serviceNameOrId, InputStream tarStreamFile,
            String completePresenterPath) throws Exception {
        // dockerService.file
        dockerService.copyFileToContainer(serviceNameOrId, tarStreamFile,
                completePresenterPath);
    }

    @Override
    public List<String> getFolderFilesList(String containerId,
            String remotePath, String filter) throws Exception {
        return dockerService.getFilesListFromContainerFolder(containerId,
                remotePath, filter);
    }
}
