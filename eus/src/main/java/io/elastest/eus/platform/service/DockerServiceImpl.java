package io.elastest.eus.platform.service;

import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.HostConfig.Bind;
import com.spotify.docker.client.messages.HostConfig.Bind.Builder;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.model.DockerPullImageProgress;
import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.service.DockerService;
import io.elastest.epm.client.service.EpmService;
import io.elastest.eus.json.WebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusJsonService;
import io.elastest.eus.service.SessionService;
import io.elastest.eus.session.SessionInfo;

@Service
public class DockerServiceImpl implements PlatformService {
    final Logger logger = getLogger(lookup().lookupClass());

    @Value("${container.recording.folder}")
    private String containerRecordingFolder;
    @Value("${hub.exposedport}")
    private int hubExposedPort;
    @Value("${hub.vnc.exposedport}")
    private int hubVncExposedPort;
    @Value("${hub.novnc.exposedport}")
    private int noVncExposedPort;
    @Value("${browser.screen.resolution}")
    private String browserScreenResolution;
    @Value("${browser.shm.size}")
    private long shmSize;
    @Value("${novnc.html}")
    private String vncHtml;
    @Value("${hub.vnc.password}")
    private String hubVncPassword;
    @Value("${et.host.env}")
    private String etHostEnv;
    @Value("${et.host.type.env}")
    private String etHostEnvType;
    @Value("${use.torm}")
    private boolean useTorm;
    @Value("${ws.dateformat}")
    private String wsDateFormat;

    private DockerService dockerService;
    private SessionService sessionService;
    private EusJsonService jsonService;

    public DockerServiceImpl(DockerService dockerService,
            SessionService sessionService, EusJsonService jsonService) {
        this.dockerService = dockerService;
        this.sessionService = sessionService;
        this.jsonService = jsonService;
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
    public String generateRandomContainerNameWithPrefix(String prefix) {
        return prefix + randomUUID().toString();
    }

    @Override
    public void buildAndRunBrowserInContainer(SessionInfo sessionInfo,
            String containerPrefix, String originalRequestBody,
            String folderPath, String network, List<String> additionalNetworks,
            List<String> envs, Map<String, String> labels,
            DesiredCapabilities capabilities, String imageId) throws Exception {
        String hubContainerName = generateRandomContainerNameWithPrefix(
                containerPrefix);
        // Recording Volume
        List<Bind> volumes = new ArrayList<>();
        logger.info("Folder path in host: {}", folderPath);
        Builder dockerSockVolumeBuilder = Bind.builder();
        dockerSockVolumeBuilder.from(folderPath);
        dockerSockVolumeBuilder.to(containerRecordingFolder);

        volumes.add(dockerSockVolumeBuilder.build());

        // Port binding
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

        // Exposed ports
        List<String> exposedPorts = asList(exposedHubPort, exposedVncPort,
                exposedNoVncPort);

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

        if (useTorm) {
            dockerBuilder.network(network);
        }
        // Save info into SessionInfo
        sessionInfo.setHubContainerName(hubContainerName);
        SimpleDateFormat dateFormat = new SimpleDateFormat(wsDateFormat);
        sessionInfo.setCreationTime(dateFormat.format(new Date()));
        sessionInfo.setHubBindPort(hubPort);
        sessionInfo.setHubVncBindPort(hubPort);

        String testName = jsonService
                .jsonToObject(originalRequestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().getTestName();
        sessionInfo.setTestName(testName);

        boolean manualRecording = jsonService
                .jsonToObject(originalRequestBody, WebDriverCapabilities.class)
                .getDesiredCapabilities().isManualRecording();
        sessionInfo.setManualRecording(manualRecording);
        sessionInfo.setStatus(DockerServiceStatusEnum.INITIALIZING);
        sessionInfo.setStatusMsg("Initializing...");
        sessionService.sendNewSessionToAllClients(sessionInfo, false);

        // Pull
        dockerService.pullImageWithProgressHandler(imageId,
                getBrowserProgressHandler(imageId, sessionInfo));

        sessionInfo.setStatus(DockerServiceStatusEnum.STARTING);
        sessionInfo.setStatusMsg("Starting...");
        sessionService.sendNewSessionToAllClients(sessionInfo, false);

        // Start
        String containerId = dockerService.createAndStartContainerWithPull(
                dockerBuilder.build(), EpmService.etMasterSlaveMode, true);

        // Additional Networks
        if (additionalNetworks != null && additionalNetworks.size() > 0) {
            logger.debug(
                    "Inserting browser container into additional networks");
            for (String additionalNetwork : additionalNetworks) {
                if (additionalNetwork != null
                        && !"".equals(additionalNetwork)) {
                    logger.debug("Inserting browser container into {} network",
                            additionalNetwork);
                    dockerService.insertIntoNetwork(additionalNetwork,
                            containerId);
                }
            }
        }

        // Wait Reachable
        String hubPath = "/wd/hub";
        String hubIp = dockerService.getDockerServerIp();
        String hubUrl = "http://" + hubIp + ":" + hubPort + hubPath;

        // Save hub url into Session Info
        sessionInfo.setHubUrl(hubUrl);

        logger.debug("Container: {} -- Hub URL: {}", hubContainerName, hubUrl);

        String vncUrlFormat = "http://%s:%d/" + vncHtml
                + "?resize=scale&autoconnect=true&password=" + hubVncPassword;
        String vncUrl = format(vncUrlFormat, hubIp, noVncBindedPort);
        String internalVncUrl = vncUrl;

        String etHost = getenv(etHostEnv);
        String etHostType = getenv(etHostEnvType);
        if (etHostType != null && etHost != null) {
            // If server-address
            if (!"default".equalsIgnoreCase(etHostType)) {
                hubIp = etHost;
                vncUrl = format(vncUrlFormat, hubIp, noVncBindedPort);
            }
        }

        dockerService.waitForHostIsReachable(internalVncUrl);

        sessionInfo.setVncContainerName(hubContainerName);
        sessionInfo.setVncUrl(vncUrl);
        sessionInfo.setNoVncBindPort(noVncBindedPort);

        sessionInfo.setStatus(DockerServiceStatusEnum.READY);

    }

    private ProgressHandler getBrowserProgressHandler(String image,
            SessionInfo sessionInfo) {
        DockerPullImageProgress dockerPullImageProgress = new DockerPullImageProgress();
        dockerPullImageProgress.setImage(image);
        dockerPullImageProgress.setCurrentPercentage(0);

        sessionInfo.setStatus(DockerServiceStatusEnum.PULLING);
        sessionInfo.setStatusMsg("Pulling " + image + " image");
        return new ProgressHandler() {
            @Override
            public void progress(ProgressMessage message)
                    throws DockerException {
                dockerPullImageProgress.processNewMessage(message);
                String msg = "Pulling image " + image + ": "
                        + dockerPullImageProgress.getCurrentPercentage() + "%";

                sessionInfo.setStatusMsg(msg);
                try {
                    sessionService.sendNewSessionToAllClients(sessionInfo,
                            false);
                } catch (IOException e) {
                    logger.error("Error on send session {} info: ",
                            sessionInfo.getSessionId(), e);
                }
            }

        };

    }
    
    public void execCommand(String hubContainerName, boolean awaitCompletion,
            String... command) throws Exception {
        dockerService.execCommand(hubContainerName, awaitCompletion, command);
    }
}
