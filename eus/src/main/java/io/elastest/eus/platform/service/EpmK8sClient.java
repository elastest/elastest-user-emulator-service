package io.elastest.eus.platform.service;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.service.K8Service;
import io.elastest.epm.client.service.K8Service.PodInfo;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.session.SessionInfo;

public class EpmK8sClient extends PlatformService {
    final Logger logger = getLogger(lookup().lookupClass());

    @Value("${host.shared.files.relative.folder}")
    private String hostSharedFilesRelativeFolder;
    @Value("${container.recording.folder}")
    private String containerRecordingFolder;
    @Value("${container.recording.folder}")
    private String containerSharedFilesFolder;

    @Autowired
    private K8Service k8sService;
    @Autowired
    private EusFilesService eusFilesService;

    @Override
    public List<String> getContainerNetworksByContainerPrefix(String prefix)
            throws Exception {
        // TODO Auto-generated method stub
        return null;
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

        String exposedHubPort = Integer.toString(hubExposedPort);
        String exposedVncPort = Integer.toString(hubVncExposedPort);
        String exposedNoVncPort = Integer.toString(noVncExposedPort);

        String recordingsPath = createRecordingsPath(folderPath);
        dockerBrowserInfo.setHostSharedFilesFolderPath(recordingsPath);
        ((SessionInfo) dockerBrowserInfo).setFolderPath(recordingsPath);

        eusFilesService.createFolderIfNotExists(recordingsPath);

        logger.debug("**** Paths for recordings ****");
        logger.debug("Host path: {}", recordingsPath);
        logger.debug("Path in container: {}", containerSharedFilesFolder);

        /* **** Exposed ports **** */
        List<String> exposedPorts = asList(exposedHubPort, exposedVncPort,
                exposedNoVncPort);

        /* **** Docker Builder **** */
        DockerBuilder dockerBuilder = new DockerBuilder(imageId);
        dockerBuilder.containerName(hubContainerName);
        dockerBuilder.exposedPorts(exposedPorts);
        dockerBuilder.shmSize(shmSize);
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

        /* **** Set IPs and ports **** */
        dockerBrowserInfo.setHubIp(podInfo.getPodIp());
        dockerBrowserInfo.setHubPort(hubExposedPort);
        dockerBrowserInfo.setNoVncBindedPort(noVncExposedPort);

    }

    public String createRecordingsPath(String hostPath) {
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
        k8sService.deletePod(podName);

    }

    @Override
    public void waitForBrowserReady(String internalVncUrl,
            DockerBrowserInfo dockerBrowserInfo) throws Exception {
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
            DockerBrowserInfo dockerBrowserInfo) throws IOException {
        k8sService.copyFileFromContainer(dockerBrowserInfo.getBrowserPod(),
                containerRecordingFolder,
                dockerBrowserInfo.getHostSharedFilesFolderPath());
        File recordingsDirectory = new File(
                dockerBrowserInfo.getHostSharedFilesFolderPath()
                        + containerRecordingFolder);
        moveFiles(recordingsDirectory,
                dockerBrowserInfo.getHostSharedFilesFolderPath());
    }

    private void moveFiles(File fileToMove, String targetPath)
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
