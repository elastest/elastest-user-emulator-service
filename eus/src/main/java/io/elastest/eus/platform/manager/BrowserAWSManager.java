package io.elastest.eus.platform.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import io.elastest.epm.client.model.DockerServiceStatus.DockerServiceStatusEnum;
import io.elastest.epm.client.utils.UtilTools;
import io.elastest.eus.api.model.ExecutionData;
import io.elastest.eus.config.EusContextProperties;
import io.elastest.eus.json.AWSConfig;
import io.elastest.eus.json.AWSConfig.AWSInstancesConfig;
import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.json.WebDriverCapabilities.DesiredCapabilities;
import io.elastest.eus.service.EusFilesService;
import io.elastest.eus.services.model.BrowserSync;
import io.elastest.eus.services.model.WebRTCQoEMeter;
import io.elastest.eus.session.SessionManager;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

public class BrowserAWSManager extends PlatformManager {

    AWSClient awsClient;

    public BrowserAWSManager(AWSClient awsClient, EusFilesService eusFilesService,
            EusContextProperties contextProperties) {
        super(eusFilesService, contextProperties);
        this.awsClient = awsClient;
    }

    public BrowserAWSManager(Region region, String secretAccessKey, String accessKeyId,
            String sshUser, String sshPrivateKey, EusFilesService eusFilesService,
            EusContextProperties contextProperties) {
        super(eusFilesService, contextProperties);
        this.awsClient = new AWSClient(region, secretAccessKey, accessKeyId, sshUser,
                sshPrivateKey);
    }

    public BrowserAWSManager(AWSConfig awsConfig, EusFilesService eusFilesService,
            EusContextProperties contextProperties) {
        super(eusFilesService, contextProperties);
        this.awsClient = new AWSClient(awsConfig);
    }

    @Override
    public void downloadFileOrFilesFromServiceToEus(String instanceId, String remotePath,
            String localPath, String originalFilename, String newFilename, Boolean isDirectory)
            throws Exception {
        if (isDirectory) {
            awsClient.downloadFolderFiles(instanceId, remotePath, localPath);
        } else {
            awsClient.downloadFile(instanceId, remotePath, originalFilename, localPath);
        }
    }

    @Override
    public void downloadFileOrFilesFromSubServiceToEus(String instanceId, String subServiceID,
            String remotePath, String localPath, String originalFilename, String newFilename,
            Boolean isDirectory) throws Exception {
        String instanceCompleteFilePath = "/tmp/";
        if (isDirectory) {
            awsClient.executeCommand(instanceId, "docker cp " + subServiceID + ":" + remotePath
                    + " " + instanceCompleteFilePath);
            awsClient.downloadFolderFiles(instanceId, instanceCompleteFilePath, localPath);
        } else {
            remotePath = remotePath.endsWith(EusFilesService.FILE_SEPARATOR) ? remotePath
                    : remotePath + EusFilesService.FILE_SEPARATOR;
            // Copy from container to instance first
            awsClient.executeCommand(instanceId, "docker cp " + subServiceID + ":" + remotePath
                    + originalFilename + " " + instanceCompleteFilePath + newFilename);

            downloadFileOrFilesFromServiceToEus(instanceId, instanceCompleteFilePath, localPath,
                    newFilename, newFilename, false);
        }
    }

    @Override
    public InputStream getFileFromService(String instanceId, String path, Boolean isDirectory)
            throws Exception {
        if (isDirectory) {
            // TODO return files in folder
            return null;
        } else {
            return awsClient.getFileAsInputStream(instanceId, path);
        }
    }

    @Override
    public InputStream getFileFromSubService(String instanceId, String subServiceID,
            String completeFilePath, Boolean isDirectory) throws Exception {
        if (isDirectory) {
            // TODO return files in folder
            return null;
        } else {
            String fileName = eusFilesService.getFileNameFromCompleteFilePath(completeFilePath);
            String instanceCompleteFilePath = "/tmp/" + fileName;

            // Copy from container to instance first
            awsClient.executeCommand(instanceId, "docker cp " + subServiceID + ":"
                    + completeFilePath + " " + instanceCompleteFilePath);

            return awsClient.getFileAsInputStream(instanceId, instanceCompleteFilePath);
        }
    }

    @Override
    public void copyFilesFromBrowserIfNecessary(SessionManager sessionManager) throws Exception {
        String remotePath = contextProperties.CONTAINER_RECORDING_FOLDER;
        String localPath = eusFilesService.getSessionFilesFolderBySessionManager(sessionManager);
        downloadFileOrFilesFromServiceToEus(sessionManager.getAwsInstanceId(), remotePath,
                localPath, null, null, true);
    }

    @Override
    public String getSessionContextInfo(SessionManager sessionManager) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    private Instance provideInstance(SessionManager sessionManager, String amiId,
            Integer volumeSizeInGiB) throws Exception {
        AWSInstancesConfig awsInstanceConfig = sessionManager.getCapabilities().getAwsConfig()
                .getAwsInstancesConfig();

        // IMAGE_ID
        if (amiId == null || "".equals(amiId)) {
            amiId = awsInstanceConfig.getAmiId();
        }
        // INSTANCE_TYPE
        InstanceType instanceType = awsInstanceConfig.getInstanceType();
        // KEY_NAME
        String keyName = awsInstanceConfig.getKeyName();
        // SECURITY_GROUP
        Collection<String> securityGroups = awsInstanceConfig.getSecurityGroups();
        // 'ResourceType=instance,Tags=[{Key=Type,Value=OpenViduLoadTest}]'
        Collection<TagSpecification> tagSpecifications = awsInstanceConfig.getTagSpecifications();

        // Call to AwsClient to create instances
        Instance instance = awsClient.provideInstance(amiId, instanceType, keyName, securityGroups,
                tagSpecifications, volumeSizeInGiB);
        return instance;
    }

    private Instance provideInstance(SessionManager sessionManager, Integer volumeSizeInGiB)
            throws Exception {
        return provideInstance(sessionManager, null, volumeSizeInGiB);
    }

    private Instance provideInstance(SessionManager sessionManager) throws Exception {
        return provideInstance(sessionManager, null);
    }

    private String provideAndWaitForInstance(SessionManager sessionManager, String amiId,
            Integer volumeSizeInGiB) throws Exception, TimeoutException {
        // Call to AwsClient to create instances
        Instance instance = provideInstance(sessionManager, amiId, volumeSizeInGiB);
        // Wait
        awsClient.waitForInstance(instance, 600);
        instance = awsClient.describeInstance(instance);

        String instanceId = instance.instanceId();
        return instanceId;
    }

    private String provideAndWaitForInstance(SessionManager sessionManager, Integer volumeSizeInGiB)
            throws Exception, TimeoutException {
        return provideAndWaitForInstance(sessionManager, null, volumeSizeInGiB);
    }

    @Override
    public void buildAndRunBrowserInContainer(SessionManager sessionManager, String containerPrefix,
            String originalRequestBody, String folderPath, ExecutionData execData,
            List<String> envs, Map<String, String> labels, DesiredCapabilities capabilities,
            String imageId) throws Exception {
        sessionManager.setStatus(DockerServiceStatusEnum.INITIALIZING);
        sessionManager.setStatusMsg("Initializing...");

        String recordingsPath = createRecordingsPath(folderPath);
        sessionManager.setHostSharedFilesFolderPath(recordingsPath);
        ((SessionManager) sessionManager).setFolderPath(recordingsPath);

        sessionManager.setStatus(DockerServiceStatusEnum.STARTING);
        sessionManager.setStatusMsg("Starting...");

        Instance instance = provideInstance(sessionManager);

        // Wait
        awsClient.waitForInstance(instance, 600);
        instance = awsClient.describeInstance(instance);

        String hubIp = instance.publicDnsName();

        if (hubIp == null || "".equals(hubIp)) {
            hubIp = instance.publicIpAddress();
        }

        String instanceId = instance.instanceId();
        sessionManager.setAwsInstanceId(instanceId);

        sessionManager.setHubIp(hubIp);
        sessionManager.setHubPort(contextProperties.HUB_EXPOSED_PORT);
        sessionManager.setNoVncBindedPort(contextProperties.NO_VNC_EXPOSED_PORT);

        String browserServiceName = getBrowserServiceName(instanceId);
        sessionManager.setHubContainerName(browserServiceName);
        sessionManager.setVncContainerName(browserServiceName);
    }

    @Override
    public BrowserSync buildAndRunBrowsersyncService(SessionManager sessionManager,
            ExecutionData execData, CrossBrowserWebDriverCapabilities crossBrowserCapabilities,
            Map<String, String> labels) throws Exception {
        BrowserSync browserSync = new BrowserSync(crossBrowserCapabilities);

        String instanceId = provideAndWaitForInstance(sessionManager, null);
        browserSync.setIdentifier(instanceId);

        return browserSync;
    }

    @Override
    public WebRTCQoEMeter buildAndRunWebRTCQoEMeterService(SessionManager sessionManager,
            ExecutionData execData, Map<String, String> labels) throws Exception {
        WebRTCQoEMeter webRTCQoEMeter = new WebRTCQoEMeter();
        String serviceContainerName = getWebRTCQoEMeterServiceName(execData);
        webRTCQoEMeter.setIdentifier(serviceContainerName);

        // TODO use ubuntu base image WITH DOCKER
        // String instanceId = provideAndWaitForInstance(sessionManager,
        // awsClient.getUbuntu16AmiImageId());

        String instanceId = provideAndWaitForInstance(sessionManager, new Integer(20));

        awsClient.executeCommand(instanceId,
                "docker pull " + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_NAME);

        awsClient.executeCommand(instanceId,
                "docker run -d --name " + serviceContainerName + " "
                        + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_NAME + " "
                        + contextProperties.EUS_SERVICE_WEBRTC_QOE_METER_IMAGE_COMMAND);

        webRTCQoEMeter.setAwsInstanceId(instanceId);

        return webRTCQoEMeter;
    }

    @Override
    public String execCommand(String instanceId, String command) throws Exception {
        return awsClient.executeCommand(instanceId, command);
    }

    @Override
    public String execCommandInSubService(String instanceId, String subserviceId,
            boolean awaitCompletion, String command) throws Exception {
        if (command != null) {
            // Commands executed in browser container
            String mergedCommand = "docker exec -t ";
            if (!awaitCompletion) {
                mergedCommand += "-d ";
            }
            mergedCommand += subserviceId + " sh -c '" + command + "'";

            return execCommand(instanceId, mergedCommand);
        }
        return null;
    }

    @Override
    public String execCommandInBrowser(String instanceId, boolean awaitCompletion,
            String... command) throws Exception {
        if (command != null) {
            String browserServiceName = getBrowserServiceName(instanceId) + " ";

            List<String> commandListAux = Arrays.asList(command);

            String mergedCommand = StringUtils.join(commandListAux, " ");

            return execCommandInSubService(instanceId, browserServiceName, awaitCompletion,
                    mergedCommand);
        }
        return null;
    }

    @Override
    public boolean existServiceWithName(String name) throws Exception {
        return awsClient.describeInstance(name) != null;
    }

    @Override
    public void removeServiceWithTimeout(String instanceId, int killAfterSeconds) throws Exception {
        awsClient.terminateInstance(instanceId);
    }

    @Override
    public void waitForBrowserReady(String internalVncUrl, SessionManager sessionManager)
            throws Exception {
        try {
            UtilTools.waitForHostIsReachable(internalVncUrl, 45);
            sessionManager.setStatusMsg("Ready");
            sessionManager.setStatus(DockerServiceStatusEnum.READY);
        } catch (Exception e) {
            logger.error("Error on wait for host reachable: {}", e.getMessage());
            removeServiceWithTimeout(sessionManager.getAwsInstanceId(), 60);
            throw e;
        }
        // Wait some seconds for Hub (4444) ready
        Thread.sleep(5000);
    }

    public String getBrowserServiceName(String instanceId) throws Exception {
        String command = "docker ps -a | grep elastestbrowser | awk '{print $1}' | tr -d '\\n'";
        return awsClient.executeCommand(instanceId, command);
    }

    @Override
    public void uploadFile(SessionManager sessionManager, String instanceId,
            InputStream inputStreamFile, String completeFilePath, String fileName)
            throws Exception {
        awsClient.uploadFile(instanceId, completeFilePath, fileName, inputStreamFile);
    }

    @Override
    public void uploadFileToSubservice(SessionManager sessionManager, String instanceId,
            String subServiceID, InputStream inputStreamFile, String completeFilePath,
            String fileName) throws Exception {
        String instancePath = "/tmp/";
        // first upload to instance
        uploadFile(sessionManager, instanceId, inputStreamFile, instancePath, fileName);

        logger.debug("File {} uploaded to instance {} at {}. Copying to subservice {}", fileName,
                instanceId, instancePath, subServiceID);

        completeFilePath = completeFilePath.endsWith(EusFilesService.FILE_SEPARATOR)
                ? completeFilePath
                : completeFilePath + EusFilesService.FILE_SEPARATOR;

        // After copy into subservice
        awsClient.executeCommand(instanceId, "docker cp " + instancePath + fileName + " "
                + subServiceID + ":" + completeFilePath + fileName);

        // last remove from instance
        awsClient.executeCommand(instanceId, "rm " + instancePath + fileName);
    }

    @Override
    public void uploadFileFromEus(SessionManager sessionManager, String serviceNameOrId,
            String filePathInEus, String fileNameInEus, String targetFilePath,
            String targetFileName) throws Exception {
        String completeFilePathInEUS = (filePathInEus.endsWith(EusFilesService.FILE_SEPARATOR)
                ? filePathInEus
                : filePathInEus + EusFilesService.FILE_SEPARATOR) + fileNameInEus;

        File fileInEus = new File(completeFilePathInEUS);
        FileInputStream fileISInEus = new FileInputStream(fileInEus);

        uploadFile(sessionManager, serviceNameOrId, fileISInEus, targetFilePath, targetFileName);
        try {
            logger.debug("Removing {} file from EUS after upload to service", filePathInEus);
            fileInEus.delete();

            File folder = new File(targetFilePath);
            if (folder.isDirectory() && folder.list().length == 0) {
                logger.debug(
                        "Removing {} folder (because is empty) from EUS after upload to service",
                        targetFilePath);
                fileInEus.delete();
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void uploadFileToSubserviceFromEus(SessionManager sessionManager, String instanceId,
            String subServiceID, String filePathInEus, String fileNameInEus, String targetFilePath,
            String targetFileName) throws Exception {
        String completeFilePathInEUS = (filePathInEus.endsWith(EusFilesService.FILE_SEPARATOR)
                ? filePathInEus
                : filePathInEus + EusFilesService.FILE_SEPARATOR) + fileNameInEus;

        File fileInEus = new File(completeFilePathInEUS);
        FileInputStream fileISInEus = new FileInputStream(fileInEus);
        uploadFileToSubservice(sessionManager, instanceId, subServiceID, fileISInEus,
                targetFilePath, targetFileName);
    }

    @Override
    public Boolean uploadFileToBrowser(SessionManager sessionManager, ExecutionData execData,
            MultipartFile file, String completeFilePath) throws Exception {
        // If not path, upload file to et shared files folder (copying directly
        // to instance volume folder shared with browser container)
        if (completeFilePath == null || "".equals(completeFilePath)) {
            completeFilePath = eusFilesService.getEusSharedFilesPath(sessionManager);
            uploadFile(sessionManager, sessionManager.getAwsInstanceId(), file.getInputStream(),
                    completeFilePath, file.getOriginalFilename());
        } else {
            uploadFileToSubservice(sessionManager, sessionManager.getAwsInstanceId(),
                    sessionManager.getVncContainerName(), file.getInputStream(), completeFilePath,
                    file.getOriginalFilename());
        }
        return true;
    }

    @Override
    public Boolean uploadFileFromUrlToBrowser(SessionManager sessionManager, ExecutionData execData,
            String fileUrl, String completeFilePath, String fileName) throws Exception {
        // If not path, upload file to et shared files folder (copying directly
        // to instance volume folder shared with browser container)
        if (completeFilePath == null || "".equals(completeFilePath)) {
            completeFilePath = eusFilesService.getEusSharedFilesPath(sessionManager);
            File file = eusFilesService.saveFileFromUrlToPathInEUS(completeFilePath, fileName,
                    fileUrl);
            FileInputStream fileIS = new FileInputStream(file);

            uploadFile(sessionManager, sessionManager.getAwsInstanceId(), fileIS, completeFilePath,
                    fileName);

            // Remove temporal file from EUS
            try {
                file.delete();
            } catch (Exception e) {
            }
        } else {

            String pathInEus = completeFilePath.endsWith(EusFilesService.FILE_SEPARATOR)
                    ? completeFilePath
                    : completeFilePath + EusFilesService.FILE_SEPARATOR;
            pathInEus += sessionManager.getSessionId() + EusFilesService.FILE_SEPARATOR;

            File file = eusFilesService.saveFileFromUrlToPathInEUS(pathInEus, fileName, fileUrl);
            InputStream fileIS = new FileInputStream(file);

            uploadFileToSubservice(sessionManager, sessionManager.getAwsInstanceId(),
                    sessionManager.getVncContainerName(), fileIS, completeFilePath, fileName);

            // Remove temporal file from EUS
            try {
                file.delete();
            } catch (Exception e) {
            }
        }
        return true;
    }

    @Override
    public List<String> getFolderFilesList(String instanceId, String remotePath, String filter)
            throws Exception {
        return awsClient.listFolderFiles(instanceId, remotePath, filter, null);
    }

    @Override
    public List<String> getSubserviceFolderFilesList(String instanceId, String subServiceId,
            String remotePath, String filter) throws Exception {
        return awsClient.listFolderFiles(instanceId, remotePath, filter, subServiceId);
    }
}
