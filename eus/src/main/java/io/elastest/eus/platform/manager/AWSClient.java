package io.elastest.eus.platform.manager;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import io.elastest.eus.json.AWSConfig;
import io.elastest.eus.utils.ScpFileDownloader;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest.Builder;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.MonitorInstancesRequest;
import software.amazon.awssdk.services.ec2.model.MonitorInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ec2.model.SummaryStatus;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.ec2.model.UnmonitorInstancesRequest;

public class AWSClient {
    final static Logger logger = getLogger(lookup().lookupClass());

    Ec2Client ec2;
    AWSConfig awsConfig;

    public AWSClient(AWSConfig awsConfig) {
        this.awsConfig = awsConfig;
        AwsCredentials credentials = AwsBasicCredentials.create(
                awsConfig.getAccessKeyId(), awsConfig.getSecretAccessKey());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider
                .create(credentials);
        ec2 = Ec2Client.builder().credentialsProvider(credentialsProvider)
                .region(awsConfig.getRegion()).build();

    }

    public AWSClient(Region region, String secretAccessKey, String accessKeyId,
            String sshUser, String sshPrivateKey) {
        this(new AWSConfig(region, secretAccessKey, accessKeyId, sshUser,
                sshPrivateKey));
    }

    public List<Instance> provideInstances(String amiId,
            InstanceType instanceType, String keyName,
            Collection<String> securityGroups,
            Collection<TagSpecification> tagSpecifications, Integer maxCount,
            Integer minCount) throws Exception {
        RunInstancesRequest run_request = RunInstancesRequest.builder()
                .imageId(amiId).instanceType(instanceType).keyName(keyName)
                .securityGroups(securityGroups)
                .tagSpecifications(tagSpecifications).maxCount(maxCount)
                .minCount(minCount).build();

        RunInstancesResponse response = ec2.runInstances(run_request);
        return response.instances();
    }

    public Instance provideInstance(String amiId, InstanceType instanceType,
            String keyName, Collection<String> securityGroups,
            Collection<TagSpecification> tagSpecifications) throws Exception {
        return provideInstances(amiId, instanceType, keyName, securityGroups,
                tagSpecifications, 1, 1).get(0);
    }

    public void waitForInstance(Instance instance, int timeoutSeconds)
            throws TimeoutException {
        long endWaitTime = System.currentTimeMillis() + timeoutSeconds * 1000;

        boolean isRunningCompletely = instanceIsRunningAndInitializedCompletely(
                instance);
        while (System.currentTimeMillis() < endWaitTime
                && !isRunningCompletely) {
            logger.debug("Waiting for instance with ID {}. (Timeout: {}s)",
                    instance.instanceId(), timeoutSeconds);
            instance = describeInstance(instance.instanceId());
            isRunningCompletely = instanceIsRunningAndInitializedCompletely(
                    instance);
            if (isRunningCompletely) {
                break;
            } else {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }
        if (!isRunningCompletely) {
            throw new TimeoutException("Timeout (" + timeoutSeconds
                    + "s) during waiting for AWS instance with ID "
                    + instance.instanceId());
        }
    }

    public boolean instanceIsRunning(Instance instance) {
        return instance != null && instance.state() != null
                && instance.state().name() == InstanceStateName.RUNNING;
    }

    public InstanceStatus getInstanceStatus(Instance instance) {
        DescribeInstanceStatusRequest statusRequest = DescribeInstanceStatusRequest
                .builder().instanceIds(instance.instanceId()).build();

        DescribeInstanceStatusResponse response = ec2
                .describeInstanceStatus(statusRequest);
        if (response != null && response.instanceStatuses().size() > 0) {
            return response.instanceStatuses().get(0);
        }
        return null;
    }

    public boolean instanceStatusIsOk(Instance instance) {
        boolean isOk = true;

        InstanceStatus status = getInstanceStatus(instance);
        if (status != null && status.instanceStatus() != null) {
            isOk = isOk && status.instanceStatus().status()
                    .equals(SummaryStatus.OK);
        } else {
            isOk = false;
        }

        return isOk;
    }

    public boolean instanceIsRunningAndInitializedCompletely(
            Instance instance) {
        return instanceIsRunning(instance) && instanceStatusIsOk(instance);
    }

    public StartInstancesResponse startInstance(String instance_id) {
        // snippet-start:[ec2.java2.start_stop_instance.start]
        StartInstancesRequest request = StartInstancesRequest.builder()
                .instanceIds(instance_id).build();

        return ec2.startInstances(request);
    }

    public StopInstancesResponse stopInstance(String instance_id) {
        // snippet-start:[ec2.java2.start_stop_instance.stop]
        StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(instance_id).build();

        return ec2.stopInstances(request);
    }

    public TerminateInstancesResponse terminateInstance(String instance_id) {
        // snippet-start:[ec2.java2.start_stop_instance.stop]
        TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                .instanceIds(instance_id).build();

        return ec2.terminateInstances(request);
    }

    public StopInstancesResponse stopInstances(List<String> instances_ids) {
        if (instances_ids != null) {
            // snippet-start:[ec2.java2.start_stop_instance.stop]
            StopInstancesRequest request = StopInstancesRequest.builder()
                    .instanceIds(instances_ids).build();

            return ec2.stopInstances(request);
        }
        return null;
    }

    /* ************************************************** */
    /* ******************** Describe ******************** */
    /* ************************************************** */

    public List<Instance> describeInstances(List<String> instancesIds,
            Collection<Filter> filters) {
        List<Instance> instances = new ArrayList<>();
        String nextToken = null;
        do {
            Builder describeInstancesRequestBuilder = DescribeInstancesRequest
                    .builder();
            if (instancesIds != null && instancesIds.size() > 0) {
                describeInstancesRequestBuilder = describeInstancesRequestBuilder
                        .instanceIds(instancesIds);
            } else {
                describeInstancesRequestBuilder = describeInstancesRequestBuilder
                        .maxResults(6);
            }
            describeInstancesRequestBuilder = describeInstancesRequestBuilder
                    .nextToken(nextToken);

            if (filters != null) {
                describeInstancesRequestBuilder = describeInstancesRequestBuilder
                        .filters(filters);
            }

            DescribeInstancesRequest request = describeInstancesRequestBuilder
                    .build();
            DescribeInstancesResponse response = ec2.describeInstances(request);

            for (Reservation reservation : response.reservations()) {
                instances.addAll(reservation.instances());
            }
            nextToken = response.nextToken();

        } while (nextToken != null);
        return instances;
    }

    public List<Instance> describeInstances(Collection<Filter> filters) {
        return describeInstances(null, filters);
    }

    public List<Instance> describeInstances() {
        return describeInstances(null, null);
    }

    public Instance describeInstance(String instanceId,
            Collection<Filter> filters) {
        return describeInstances(Arrays.asList(instanceId), filters).get(0);
    }

    public Instance describeInstance(String instanceId) {
        return describeInstance(instanceId, null);
    }

    public Instance describeInstance(Instance instance) {
        return describeInstance(instance.instanceId());
    }

    /* ************************************************* */
    /* ******************** Monitor ******************** */
    /* ************************************************* */

    public MonitorInstancesResponse monitorInstance(String instance_id) {
        // snippet-start:[ec2.java2.monitor_instance.main]
        MonitorInstancesRequest request = MonitorInstancesRequest.builder()
                .instanceIds(instance_id).build();

        return ec2.monitorInstances(request);
    }

    public void unmonitorInstance(String instance_id) {
        // snippet-start:[ec2.java2.monitor_instance.stop]
        UnmonitorInstancesRequest request = UnmonitorInstancesRequest.builder()
                .instanceIds(instance_id).build();

        ec2.unmonitorInstances(request);
    }

    /* ************************************************** */
    /* ********************* Others ********************* */
    /* ************************************************** */

    public String executeCommand(String instanceId, String command,
            int connectTimeoutSecs) throws Exception {
        logger.debug("AWS instance {} => Executing command: {}", instanceId,
                command);
        Instance instance = describeInstance(instanceId);
        JSch jsch = new JSch();

        File temp = File.createTempFile(
                "temp-privatekey-" + instanceId + "-" + randomUUID().toString(),
                ".tmp");
        temp.setWritable(true);
        temp.setReadable(true);
        temp.setExecutable(false);

        BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
        bw.write(awsConfig.getSshPrivateKey());
        bw.close();

        try {
            jsch.addIdentity(temp.getAbsolutePath());
        } catch (JSchException e) {
            logger.error(
                    "AWS instance {} => Error on execute command {}: Private Key with path {} is invalid",
                    instanceId, command, temp.getAbsolutePath());
            temp.delete();
            throw e;
        }

        String host = instance.publicIpAddress();
        int port = 22;
        String user = awsConfig.getSshUser();
        logger.debug("Execute command => Connecting to {} at {} with user {}",
                host, port, user);

        Session jschSession = jsch.getSession(user, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey");
        jschSession.setConfig(config);

        try {
            jschSession.connect(connectTimeoutSecs);
        } catch (Exception e) {
            jschSession.disconnect();
            throw e;
        }

        try {
            temp.delete();
        } catch (Exception e) {
        }

        if (jschSession.isConnected()) {
            StringBuilder outputBuffer = new StringBuilder();
            try {
                Channel channel = jschSession.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                InputStream commandOutput = channel.getInputStream();

                StringBuilder errorBuffer = new StringBuilder();
                InputStream errorOuput = ((ChannelExec) channel).getErrStream();

                channel.connect(4000);
                int readByte = commandOutput.read();
                int errorByte = errorOuput.read();
                while (readByte != 0xffffffff) {
                    outputBuffer.append((char) readByte);
                    readByte = commandOutput.read();
                }
                while (errorByte != 0xffffffff) {
                    errorBuffer.append((char) errorByte);
                    errorByte = errorOuput.read();
                }

                if (errorBuffer.length() > 0) {
                    logger.error("Error sending command '{}' to {}: {}",
                            command, host, errorBuffer.toString());
                }

                channel.disconnect();
                jschSession.disconnect();
            } catch (IOException e) {
                logger.warn(e.getMessage());
                jschSession.disconnect();
                return null;
            } catch (JSchException e) {
                logger.warn(e.getMessage());
                jschSession.disconnect();
                return null;
            }
            String result = outputBuffer.toString();
            logger.debug("AWS Command Result: {}", result);
            return result;
        } else {
            logger.error(
                    "There's no SSH connection to instance {}. Cannot send command '{}'",
                    instanceId, command);
            jschSession.disconnect();
            return null;
        }
    }

    public String executeCommand(String instanceId, String command)
            throws Exception {
        return executeCommand(instanceId, command, 120000);
    }

    /* ************************************************* */
    /* ********************* FILES ********************* */
    /* ************************************************* */

    public void downloadFile(String instanceId, String remotePath,
            String filename, String localPath) {
        Instance instance = describeInstance(instanceId);

        ScpFileDownloader fileDownloader = new ScpFileDownloader(
                awsConfig.getSshUser(),
                instance.publicDnsName() != null ? instance.publicDnsName()
                        : instance.publicIpAddress(),
                awsConfig.getSshPrivateKey());
        fileDownloader.downloadFile(remotePath, filename, localPath);
    }

    public List<String> listFolderFiles(String instanceId, String remotePath) {
        String command = "ls -p " + remotePath + " | grep -v / | tr '\\n' ','";
        List<String> filesNames = null;
        try {
            String response = executeCommand(instanceId, command);

            if (response != null) {
                try {
                    String[] filesNamesArr = response.split(",");
                    filesNames = Arrays.asList(filesNamesArr);
                } catch (Exception e) {
                    logger.error(
                            "Error on get names of files from response: {}",
                            response);
                }
            }

        } catch (Exception e1) {
            logger.error("Error on get names of files: {}", e1.getMessage());
        }

        return filesNames;
    }

    public void downloadFolderFiles(String instanceId, String remotePath,
            String localPath) {
        List<String> filesNames = listFolderFiles(instanceId, remotePath);
        if (filesNames != null) {
            for (String fileName : filesNames) {
                downloadFile(instanceId, remotePath, fileName, localPath);
            }
        }
    }

}
