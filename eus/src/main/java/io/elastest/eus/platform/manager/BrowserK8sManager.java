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
import org.springframework.web.multipart.MultipartFile;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.service.K8sService;
import io.elastest.epm.client.service.K8sService.PodInfo;
import io.elastest.epm.client.service.K8sService.ServiceInfo;
import io.elastest.epm.client.utils.UtilTools;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.EusContextProperties;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.services.model.WebRTCQoEMeter;
import io.elastest.eus.session.SessionManager;

public class BrowserK8sManager extends PlatformManager {
    final Logger logger = getLogger(lookup().lookupClass());

    private K8sService k8sService;

    public BrowserK8sManager(K8sService k8sService, EusFilesService eusFilesService,
            EusContextProperties contextProperties) {
        super(eusFilesService, contextProperties);
        this.k8sService = k8sService;
    }

    @SuppressWarnings("static-access")
    @Override
    public void buildAndRunBrowserInContainer(SessionManager sessionManager, String containerPrefix,
            String originalRequestBody, String folderPath, ExecutionData execData,
            List<String> envs, Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception {
        String hubContainerName = generateRandomContainerNameWithPrefix(containerPrefix, execData);

        String exposedHubPort = Integer.toString(contextProperties.HUB_EXPOSED_PORT);
        String exposedVncPort = Integer.toString(contextProperties.HUB_VNC_EXPOSED_PORT);
        String exposedNoVncPort = Integer.toString(contextProperties.NO_VNC_EXPOSED_PORT);

        String recordingsPath = createRecordingsPath(folderPath);
        sessionManager.setHostSharedFilesFolderPath(recordingsPath);
        ((SessionManager) sessionManager).setFolderPath(recordingsPath);

        eusFilesService.createFolderIfNotExists(recordingsPath);

        logger.debug("**** Paths for recordings ****");
        logger.debug("Host path: {}", recordingsPath);
        logger.debug("Path in container: {}", contextProperties.CONTAINER_SHARED_FILES_FOLDER);

        /* **** Exposed ports **** */
        List<String> exposedPorts = asList(exposedHubPort, exposedVncPort, exposedNoVncPort);

        // Add extra labels
        labels.put(k8sService.LABEL_POD_NAME, hubContainerName);

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(imageId);
        dockerBuilder.containerName(hubContainerName);
        dockerBuilder.exposedPorts(exposedPorts);
        dockerBuilder.shmSize(contextProperties.SHM_SIZE);
        dockerBuilder.envs(envs);
        dockerBuilder.capAdd(asList("SYS_ADMIN"));
        dockerBuilder.labels(labels);

        if (capabilities.getExtraHosts() != null) {
            dockerBuilder.extraHosts(capabilities.getExtraHosts());
        }

        /* **** Save info **** */
        sessionManager.setHubContainerName(hubContainerName);
        sessionManager.setVncContainerName(hubContainerName);
        sessionManager.setStatus(DockerServiceStatusEnum.INITIALIZING);
        sessionManager.setStatusMsg("Initializing...");
        sessionManager.setStatusMsg("Starting...");

        /* **** Start **** */
        String namespace = getEusNamespace();

        PodInfo podInfo = k8sService.deployPod(dockerBuilder.build(), namespace);
        sessionManager.setBrowserPod(podInfo.getPodName());

        // Binding ports
        ServiceInfo hubServiceInfo = k8sService.createService(
                hubContainerName + "-" + contextProperties.HUB_EXPOSED_PORT, hubContainerName, null,
                contextProperties.HUB_EXPOSED_PORT, "http", namespace, k8sService.LABEL_POD_NAME);
        ServiceInfo noVncServiceInfo = k8sService.createService(
                hubContainerName + "-" + contextProperties.NO_VNC_EXPOSED_PORT, hubContainerName,
                null, contextProperties.NO_VNC_EXPOSED_PORT, "http", namespace,
                k8sService.LABEL_POD_NAME);

        /* **** Set IPs and ports **** */
        sessionManager.setHubIp(hubServiceInfo.getServiceURL().getHost());
        sessionManager.setHubPort(Integer.parseInt(hubServiceInfo.getServicePort()));
        sessionManager.setNoVncBindedPort(Integer.parseInt(noVncServiceInfo.getServicePort()));
    }

    @Override
    public String execCommand(String dockerContainerIdOrName, String command) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String execCommandInSubService(String instanceId, String subserviceId,
            boolean awaitCompletion, String command) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String execCommandInBrowser(String podName, boolean awaitCompletion, String... command)
            throws Exception {
        k8sService.execCommand(k8sService.getPodByName(podName, getEusNamespace()), podName,
                awaitCompletion, command);
        return null;
    }

    @Override
    public boolean existServiceWithName(String name) throws Exception {
        return k8sService.existPodByName(name);
    }

    @Override
    public void removeServiceWithTimeout(String podName, int killAfterSeconds) throws Exception {
        String namespace = getEusNamespace();
        k8sService.deleteServiceAssociatedWithAPOD(podName, namespace);
        k8sService.deletePod(podName, namespace);

    }

    @Override
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
        // Wait some seconds for Hub (4444) ready
        Thread.sleep(5000);

    }

    @Override
    public void downloadFileOrFilesFromServiceToEus(String instanceId, String remotePath,
            String localPath, String originalFilename, String newFilename, Boolean isDirectory)
            throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadFileOrFilesFromSubServiceToEus(String instanceId, String subServiceID,
            String remotePath, String localPath, String originalFilename, String newFilename,
            Boolean isDirectory) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public InputStream getFileFromService(String serviceNameOrId, String path, Boolean isDirectory)
            throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getFileFromSubService(String instanceId, String subServiceID, String path,
            Boolean isDirectory) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSessionContextInfo(SessionManager sessionManager) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copyFilesFromBrowserIfNecessary(SessionManager sessionManager) throws IOException {
        k8sService.copyFileFromContainer(sessionManager.getBrowserPod(),
                contextProperties.CONTAINER_RECORDING_FOLDER,
                sessionManager.getHostSharedFilesFolderPath(), getEusNamespace());
        File recordingsDirectory = new File(sessionManager.getHostSharedFilesFolderPath()
                + contextProperties.CONTAINER_RECORDING_FOLDER);
        moveFiles(recordingsDirectory, sessionManager.getHostSharedFilesFolderPath());
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

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(
                contextProperties.EUS_SERVICE_BROWSERSYNC_IMAGE_NAME);
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
        String guiUrl = "http://" + ip + ":" + contextProperties.EUS_SERVICE_BROWSERSYNC_GUI_PORT;

        URL sutUrlObj = new URL(sutUrl);
        String appProtocol = sutUrlObj.getProtocol();
        String appUrl = appProtocol + "://" + ip + ":"
                + contextProperties.EUS_SERVICE_BROWSERSYNC_APP_PORT;

        browsersync.setIdentifier(serviceContainerName);
        browsersync.setGuiUrl(guiUrl);
        browsersync.setAppUrl(appUrl);

        return browsersync;
    }

    @Override
    public WebRTCQoEMeter buildAndRunWebRTCQoEMeterService(SessionManager sessionManager,
            ExecutionData execData, Map<String, String> labels) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void uploadFile(SessionManager sessionManager, String serviceNameOrId,
            InputStream tarStreamFile, String completePresenterPath, String fileName)
            throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void uploadFileToSubservice(SessionManager sessionManager, String instanceId,
            String subServiceID, InputStream tarStreamFile, String completeFilePath,
            String fileName) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void uploadFileFromEus(SessionManager sessionManager, String serviceNameOrId,
            String filePathInEus, String fileNameInEus, String targetFilePath,
            String targetFileName) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void uploadFileToSubserviceFromEus(SessionManager sessionManager, String instanceId,
            String subServiceID, String filePathInEus, String fileNameInEus, String targetFilePath,
            String targetFileName) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public Boolean uploadFileToBrowser(SessionManager sessionManager, ExecutionData execData,
            MultipartFile file, String completeFilePath) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Boolean uploadFileFromUrlToBrowser(SessionManager sessionManager, ExecutionData execData,
            String fileUrl, String completeFilePath, String fileName) throws Exception {
        // TODO
        return null;
    }

    @Override
    public List<String> getFolderFilesList(String containerId, String remotePath, String filter)
            throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getSubserviceFolderFilesList(String instanceId, String subServiceId,
            String remotePath, String filter) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public String getEusNamespace() {
        return contextProperties.ET_TSS_INSTANCE_ID;
    }
}
