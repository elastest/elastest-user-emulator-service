package io.elastest.eus.platform.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.json.AWSConfig;
import io.elastest.eus.json.AWSConfig.AWSInstancesConfig;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.session.SessionManager;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

public class AWSService extends PlatformService {

    AWSClient awsClient;

    public AWSService(AWSClient awsClient) {
        super();
        this.awsClient = awsClient;
    }

    public AWSService(URI endpoint, Region region, String secretAccessKey,
            String accessKeyId, String sshUser, String sshPrivateKey) {
        super();
        this.awsClient = new AWSClient(endpoint, region, secretAccessKey,
                accessKeyId, sshUser, sshPrivateKey);
    }

    public AWSService(AWSConfig awsConfig) {
        super();
        this.awsClient = new AWSClient(awsConfig);
    }

    @Override
    public List<String> getContainerNetworksByContainerPrefix(String prefix)
            throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getFileFromBrowser(DockerBrowserInfo dockerBrowserInfo,
            String path, Boolean isDirectory) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void copyFilesFromBrowserIfNecessary(
            DockerBrowserInfo dockerBrowserInfo, String instanceId)
            throws IOException {
        // TODO copy videos

        String remotePath = containerRecordingFolder;
        String localPath = dockerBrowserInfo.getHostSharedFilesFolderPath();

        awsClient.downloadFolderFiles(instanceId, remotePath, localPath);
    }

    @Override
    public String getSessionContextInfo(DockerBrowserInfo dockerBrowserInfo)
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
        dockerBrowserInfo.setStatus(DockerServiceStatusEnum.INITIALIZING);
        dockerBrowserInfo.setStatusMsg("Initializing...");

        String recordingsPath = createRecordingsPath(folderPath);
        dockerBrowserInfo.setHostSharedFilesFolderPath(recordingsPath);
        ((SessionManager) dockerBrowserInfo).setFolderPath(recordingsPath);

        AWSInstancesConfig awsInstanceConfig = capabilities.getAwsConfig()
                .getAwsInstancesConfig();

        // IMAGE_ID
        String amiId = awsInstanceConfig.getAmiId();
        // INSTANCE_TYPE
        InstanceType instanceType = awsInstanceConfig.getInstanceType();
        // KEY_NAME
        String keyName = awsInstanceConfig.getKeyName();
        // SECURITY_GROUP
        Collection<String> securityGroups = awsInstanceConfig
                .getSecurityGroups();
        // 'ResourceType=instance,Tags=[{Key=Type,Value=OpenViduLoadTest}]'
        Collection<TagSpecification> tagSpecifications = awsInstanceConfig
                .getTagSpecifications();

        dockerBrowserInfo.setStatus(DockerServiceStatusEnum.STARTING);
        dockerBrowserInfo.setStatusMsg("Starting...");

        // Call to AwsClient to create instances
        Instance instance = awsClient.provideInstance(amiId, instanceType,
                keyName, securityGroups, tagSpecifications);
        // Wait
        awsClient.waitForInstance(instance, 600);

        dockerBrowserInfo.setHubIp(instance.publicIpAddress());
        dockerBrowserInfo.setHubPort(hubExposedPort);
        dockerBrowserInfo.setNoVncBindedPort(noVncExposedPort);
    }

    @Override
    public void execCommand(String instanceId, boolean awaitCompletion,
            String... command) throws Exception {
        if (command != null) {
            String mergedCommand = "";

            for (String currentCommandPart : command) {
                if (!"".equals(mergedCommand)) {
                    mergedCommand += " ";
                }
                mergedCommand += currentCommandPart;
            }

            awsClient.executeCommand(instanceId, mergedCommand);
        }
    }

    @Override
    public boolean existServiceWithName(String name) throws Exception {
        return awsClient.describeInstance(name) != null;
    }

    @Override
    public void removeServiceWithTimeout(String instanceId,
            int killAfterSeconds) throws Exception {
        awsClient.terminateInstance(instanceId);
    }

    @Override
    public void waitForBrowserReady(String serviceNameOrId,
            String internalVncUrl, DockerBrowserInfo dockerBrowserInfo)
            throws Exception {
        awsClient.waitForInternalHostIsReachable(serviceNameOrId,
                internalVncUrl, 45);
        dockerBrowserInfo.setStatusMsg("Ready");
        dockerBrowserInfo.setStatus(DockerServiceStatusEnum.READY);
    }

    @Override
    public BrowserSync buildAndRunBrowsersyncService(ExecutionData execData,
            CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
