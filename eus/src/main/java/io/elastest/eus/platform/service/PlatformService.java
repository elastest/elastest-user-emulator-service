package io.elastest.eus.platform.service;

import java.util.List;
import java.util.Map;

import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.session.SessionInfo;

public interface PlatformService {

    public List<String> getContainerNetworksByContainerPrefix(String prefix)
            throws Exception;

    public String generateRandomContainerNameWithPrefix(String prefix);

    public void buildAndRunBrowserInContainer(DockerBrowserInfo dockerBrowserInfo,
            String containerPrefix, String originalRequestBody,
            String folderPath, ExecutionData execData, List<String> envs,
            Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception;

    public void execCommand(String hubContainerName, boolean awaitCompletion,
            String... command) throws Exception;

    public boolean existServiceWithName(String name) throws Exception;

    public void removeServiceWithTimeout(String containerId,
            int killAfterSeconds) throws Exception;
    
    public void waitForBrowserReady(String internalVncUrl,
            DockerBrowserInfo dockerBrowserInfo) throws Exception;

}
