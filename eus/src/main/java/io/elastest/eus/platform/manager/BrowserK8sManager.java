package io.elastest.eus.platform.manager;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.service.K8sService;
import io.elastest.epm.client.service.K8sService.PodInfo;
import io.elastest.epm.client.service.K8sService.ServiceInfo;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.ContextProperties;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.session.SessionManager;

public class BrowserK8sManager extends PlatformManager {
    final Logger logger = getLogger(lookup().lookupClass());

    private K8sService k8sService;

    public BrowserK8sManager(K8sService k8sService,
            EusFilesService eusFilesService,
            ContextProperties contextProperties) {
        super(eusFilesService, contextProperties);
        this.k8sService = k8sService;
    }

    @Override
    public List<String> getContainerNetworksByContainerPrefix(String prefix)
            throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("static-access")
    @Override
    public void buildAndRunBrowserInContainer(
            DockerBrowserInfo dockerBrowserInfo, String containerPrefix,
            String originalRequestBody, String folderPath,
            ExecutionData execData, List<String> envs,
            Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception {
        String hubContainerName = generateRandomContainerNameWithPrefix(
                containerPrefix);

        String exposedHubPort = Integer
                .toString(contextProperties.hubExposedPort);
        String exposedVncPort = Integer
                .toString(contextProperties.hubVncExposedPort);
        String exposedNoVncPort = Integer
                .toString(contextProperties.noVncExposedPort);

        String recordingsPath = createRecordingsPath(folderPath);
        dockerBrowserInfo.setHostSharedFilesFolderPath(recordingsPath);
        ((SessionManager) dockerBrowserInfo).setFolderPath(recordingsPath);

        eusFilesService.createFolderIfNotExists(recordingsPath);

        logger.debug("**** Paths for recordings ****");
        logger.debug("Host path: {}", recordingsPath);
        logger.debug("Path in container: {}",
                contextProperties.containerSharedFilesFolder);

        /* **** Exposed ports **** */
        List<String> exposedPorts = asList(exposedHubPort, exposedVncPort,
                exposedNoVncPort);

        // Add extra labels
        labels.put(k8sService.LABEL_POD_NAME, hubContainerName);

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(imageId);
        dockerBuilder.containerName(hubContainerName);
        dockerBuilder.exposedPorts(exposedPorts);
        dockerBuilder.shmSize(contextProperties.shmSize);
        dockerBuilder.envs(envs);
        dockerBuilder.capAdd(asList("SYS_ADMIN"));
        dockerBuilder.labels(labels);

        if (capabilities.getExtraHosts() != null) {
            dockerBuilder.extraHosts(capabilities.getExtraHosts());
        }

        /* **** Save info **** */
        dockerBrowserInfo.setHubContainerName(hubContainerName);
        dockerBrowserInfo.setVncContainerName(hubContainerName);
        dockerBrowserInfo.setStatus(DockerServiceStatusEnum.INITIALIZING);
        dockerBrowserInfo.setStatusMsg("Initializing...");
        dockerBrowserInfo.setStatusMsg("Starting...");

        /* **** Start **** */
        PodInfo podInfo = k8sService.deployPod(dockerBuilder.build());
        dockerBrowserInfo.setBrowserPod(podInfo.getPodName());

        // Binding ports
        ServiceInfo hubServiceInfo = k8sService.createService(hubContainerName,
                contextProperties.hubExposedPort, "http", null,
                k8sService.LABEL_POD_NAME);
        ServiceInfo noVncServiceInfo = k8sService.createService(
                hubContainerName, contextProperties.noVncExposedPort, "http",
                null, k8sService.LABEL_POD_NAME);

        /* **** Set IPs and ports **** */
        dockerBrowserInfo.setHubIp(hubServiceInfo.getServiceURL().getHost());
        dockerBrowserInfo
                .setHubPort(Integer.parseInt(hubServiceInfo.getServicePort()));
        dockerBrowserInfo.setNoVncBindedPort(
                Integer.parseInt(noVncServiceInfo.getServicePort()));

    }

    @Override
    public void execCommand(String podName, boolean awaitCompletion,
            String... command) throws Exception {
        k8sService.execCommand(k8sService.getPodByName(podName), podName,
                awaitCompletion, command);

    }

    @Override
    public boolean existServiceWithName(String name) throws Exception {
        return k8sService.existPodByName(name);
    }

    @Override
    public void removeServiceWithTimeout(String podName, int killAfterSeconds)
            throws Exception {
        k8sService.deleteService(podName, null);
        k8sService.deletePod(podName);

    }

    @Override
    public void waitForBrowserReady(String serviceNameOrId,
            String internalVncUrl, DockerBrowserInfo dockerBrowserInfo)
            throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public InputStream getFileFromBrowser(DockerBrowserInfo dockerBrowserInfo,
            String path, Boolean isDirectory) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSessionContextInfo(DockerBrowserInfo dockerBrowserInfo)
            throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copyFilesFromBrowserIfNecessary(
            DockerBrowserInfo dockerBrowserInfo, String instanceId)
            throws IOException {
        k8sService.copyFileFromContainer(dockerBrowserInfo.getBrowserPod(),
                contextProperties.containerRecordingFolder,
                dockerBrowserInfo.getHostSharedFilesFolderPath(), null);
        File recordingsDirectory = new File(
                dockerBrowserInfo.getHostSharedFilesFolderPath()
                        + contextProperties.containerRecordingFolder);
        moveFiles(recordingsDirectory,
                dockerBrowserInfo.getHostSharedFilesFolderPath());
    }

    @Override
    public BrowserSync buildAndRunBrowsersyncService(ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception {
        String serviceContainerName = generateRandomContainerNameWithPrefix(
                contextProperties.eusContainerPrefix
                        + contextProperties.eusServiceBrowsersyncPrefix);
        BrowserSync browsersync = new BrowserSync(crossBrowserCapabilities);

        DesiredCapabilities desiredCapabilities = crossBrowserCapabilities
                .getDesiredCapabilities();

        String sutUrl = crossBrowserCapabilities.getSutUrl();

        List<String> envs = new ArrayList<>();
        String optionsEnv = "BROWSER_SYNC_OPTIONS=--proxy '" + sutUrl
                + "' --open 'external'";
        envs.add(optionsEnv);

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(
                contextProperties.eusServiceBrowsersyncImageName);
        dockerBuilder.containerName(serviceContainerName);
        dockerBuilder.envs(envs);
        dockerBuilder.labels(labels);

        // ExtraHosts
        if (desiredCapabilities.getExtraHosts() != null) {
            dockerBuilder.extraHosts(desiredCapabilities.getExtraHosts());
        }

        /* **** Start **** */
        PodInfo podInfo = k8sService.deployPod(dockerBuilder.build());

        String ip = podInfo.getPodIp();
        String guiUrl = "http://" + ip + ":"
                + contextProperties.eusServiceBrowsersyncGUIPort;

        URL sutUrlObj = new URL(sutUrl);
        String appProtocol = sutUrlObj.getProtocol();
        String appUrl = appProtocol + "://" + ip + ":"
                + contextProperties.eusServiceBrowsersyncAppPort;

        browsersync.setIdentifier(serviceContainerName);
        browsersync.setGuiUrl(guiUrl);
        browsersync.setAppUrl(appUrl);

        return browsersync;
    }

}
