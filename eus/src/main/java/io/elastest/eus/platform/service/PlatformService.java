package io.elastest.eus.platform.service;

import java.util.List;
import java.util.Map;

import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.session.SessionInfo;

public interface PlatformService {

    public List<String> getContainerNetworksByContainerPrefix(String prefix)
            throws Exception;

    public String generateRandomContainerNameWithPrefix(String prefix);

    public void buildAndRunBrowserInContainer(SessionInfo sessionInfo,
            String containerPrefix, String originalRequestBody,
            String folderPath, String network, List<String> additionalNetworks,
            List<String> envs, Map<String, String> labels,
            DesiredCapabilities capabilities, String imageId) throws Exception;

    public void execCommand(String hubContainerName, boolean awaitCompletion,
            String... command) throws Exception;

}
