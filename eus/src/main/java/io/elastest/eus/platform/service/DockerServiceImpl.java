package io.elastest.eus.platform.service;

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
import org.springframework.beans.factory.annotation.Value;

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
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;

public class DockerServiceImpl extends PlatformService {
    final Logger logger = getLogger(lookup().lookupClass());

    @Value("${container.recording.folder}")
    private String containerRecordingFolder;
    @Value("${container.shared.files.folder}")
    private String containerSharedFilesFolder;
    @Value("${host.shared.files.relative.folder}")
    private String hostSharedFilesRelativeFolder;
    @Value("${browser.screen.resolution}")
    private String browserScreenResolution;
    @Value("${use.torm}")
    private boolean useTorm;
    @Value("${docker.network}")
    private String dockerNetwork;

    private DockerService dockerService;
    private EusFilesService eusFilesService;

    public DockerServiceImpl(DockerService dockerService,
            EusFilesService eusFilesService) {
        this.dockerService = dockerService;
        this.eusFilesService = eusFilesService;
    }

    @Override
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
    public InputStream getFileFromBrowser(DockerBrowserInfo dockerBrowserInfo,
            String path, Boolean isDirectory) throws Exception {
        // Note!!!: if file does not exists, spotify docker
        // returns ContainernotFoundException (bug)

        if (isDirectory) {
            return dockerService.getFilesFromContainerAsInputStreamTar(
                    dockerBrowserInfo.getVncContainerName(), path);
        } else {
            return dockerService.getSingleFileFromContainer(
                    dockerBrowserInfo.getVncContainerName(), path);
        }
    }

    @Override
    public String getSessionContextInfo(DockerBrowserInfo dockerBrowserInfo)
            throws Exception {
        String vncContainerName = dockerBrowserInfo.getVncContainerName();
        if (vncContainerName != null) {
            return dockerService.getContainerInfoStringByName(vncContainerName);
        }
        return "";
    }

    @Override
    public void buildAndRunBrowserInContainer(
            DockerBrowserInfo dockerBrowserInfo, String containerPrefix,
            String originalRequestBody, String folderPath,
            ExecutionData execData, List<String> envs,
            Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception {
        String hubContainerName = generateRandomContainerNameWithPrefix(
                containerPrefix);

        /* **** Volumes **** */
        logger.info("Folder path in host: {}", folderPath);
        List<Bind> volumes = new ArrayList<>();

        // Recording
        Builder recordingsVolumeBuilder = Bind.builder();
        recordingsVolumeBuilder.from(folderPath);
        recordingsVolumeBuilder.to(containerRecordingFolder);
        volumes.add(recordingsVolumeBuilder.build());

        // Shared files
        Builder sharedfilesVolumeBuilder = Bind.builder();
        String hostSharedFilesFolderPath = folderPath
                + (folderPath.endsWith(EusFilesService.FILE_SEPARATOR) ? ""
                        : EusFilesService.FILE_SEPARATOR)
                + hostSharedFilesRelativeFolder;

        eusFilesService.createFolderIfNotExists(hostSharedFilesFolderPath);

        sharedfilesVolumeBuilder.from(hostSharedFilesFolderPath);
        sharedfilesVolumeBuilder.to(containerSharedFilesFolder);
        volumes.add(sharedfilesVolumeBuilder.build());

        /* **** Port binding **** */
        Map<String, List<PortBinding>> portBindings = new HashMap<>();

        int hubPort = dockerService.findRandomOpenPort();
        String exposedHubPort = Integer.toString(hubExposedPort);
        portBindings.put(exposedHubPort,
                Arrays.asList(PortBinding.of("0.0.0.0", hubPort)));

        int vncPort = dockerService.findRandomOpenPort();
        String exposedVncPort = Integer.toString(hubVncExposedPort);
        portBindings.put(exposedVncPort,
                Arrays.asList(PortBinding.of("0.0.0.0", vncPort)));

        int noVncBindedPort = dockerService.findRandomOpenPort();
        String exposedNoVncPort = Integer.toString(noVncExposedPort);
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
        dockerBuilder.shmSize(shmSize);
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

        if (useTorm) {
            dockerBuilder.network(network);
        }
        /* **** Save info **** */
        dockerBrowserInfo.setHubContainerName(hubContainerName);
        dockerBrowserInfo.setVncContainerName(hubContainerName);
        dockerBrowserInfo.setStatus(DockerServiceStatusEnum.INITIALIZING);
        dockerBrowserInfo.setStatusMsg("Initializing...");

        /* **** Pull **** */
        dockerService.pullImageWithProgressHandler(imageId,
                getBrowserProgressHandler(imageId, dockerBrowserInfo));
        dockerBrowserInfo.setStatus(DockerServiceStatusEnum.STARTING);
        dockerBrowserInfo.setStatusMsg("Starting...");

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
        dockerBrowserInfo.setHubIp(dockerService.getDockerServerIp());
        dockerBrowserInfo.setHubPort(hubPort);
        dockerBrowserInfo.setNoVncBindedPort(noVncBindedPort);
    }

    public Map<String, List<String>> getNetworksFromExecutionData(
            ExecutionData execData) throws Exception {
        Map<String, List<String>> networksMap = new HashMap<>();
        String network = dockerNetwork;
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
                        network = dockerNetwork;
                        logger.error(
                                "Error on get Sut network to use with External TJob. Using default ElasTest network  {}",
                                dockerNetwork);
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
            DockerBrowserInfo dockerBrowserInfo) throws Exception {
        dockerService.waitForHostIsReachable(internalVncUrl);
        dockerBrowserInfo.setStatusMsg("Ready");
        dockerBrowserInfo.setStatus(DockerServiceStatusEnum.READY);
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
    public void copyFilesFromBrowserIfNecessary(
            DockerBrowserInfo dockerBrowserInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public BrowserSync buildAndRunBrowsersyncService(ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception {
        String serviceContainerName = generateRandomContainerNameWithPrefix(
                eusContainerPrefix + eusServiceBrowsersyncPrefix);
        BrowserSync browsersync = new BrowserSync();

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
                eusServiceBrowsersyncImageName);
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
        String guiUrl = "http://" + ip + ":" + eusServiceBrowsersyncGUIPort;

        URL sutUrlObj = new URL(sutUrl);
        String appProtocol = sutUrlObj.getProtocol();
        String appUrl = appProtocol + "://" + ip + ":"
                + eusServiceBrowsersyncAppPort;

        browsersync.setIdentifier(serviceContainerName);
        browsersync.setGuiUrl(guiUrl);
        browsersync.setAppUrl(appUrl);

        return browsersync;
    }

}
