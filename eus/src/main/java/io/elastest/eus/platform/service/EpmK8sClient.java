package io.elastest.eus.platform.service;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.HostConfig.Bind;
import com.spotify.docker.client.messages.HostConfig.Bind.Builder;

import io.elastest.epm.client.DockerContainer.DockerBuilder;
import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.service.K8Service;
import io.elastest.epm.client.service.K8Service.PodInfo;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;

public class EpmK8sClient extends PlatformService {
    final Logger logger = getLogger(lookup().lookupClass());
    
    @Value("${host.shared.files.relative.folder}")
    private String hostSharedFilesRelativeFolder;
    
    @Autowired
    private K8Service k8sService;
    
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
        PodInfo podInfo = k8sService
                .deployPod(dockerBuilder.build());

        /* **** Set IPs and ports **** */
        dockerBrowserInfo.setHubIp(podInfo.getPodIp());
        dockerBrowserInfo.setHubPort(hubExposedPort);
        dockerBrowserInfo.setNoVncBindedPort(noVncExposedPort);

    }

    @Override
    public void execCommand(String hubContainerName, boolean awaitCompletion,
            String... command) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean existServiceWithName(String name) throws Exception {
        return k8sService.existPodByName(name);
    }

    @Override
    public void removeServiceWithTimeout(String podName,
            int killAfterSeconds) throws Exception {
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

}
