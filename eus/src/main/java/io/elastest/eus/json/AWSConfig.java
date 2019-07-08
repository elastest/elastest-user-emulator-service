package io.elastest.eus.json;

import java.net.URI;
import java.util.Collection;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

public class AWSConfig {
    URI endpoint;
    Region region;
    String secretAccessKey;
    String accessKeyId;

    String sshUser;
    String sshPrivateKey;

    AWSInstancesConfig awsInstancesConfig;

    public AWSConfig() {
        // Empty default construct
    }

    public AWSConfig(URI endpoint, Region region, String secretAccessKey,
            String accessKeyId, String sshUser, String sshPrivateKey) {
        super();
        this.endpoint = endpoint;
        this.region = region;
        this.secretAccessKey = secretAccessKey;
        this.accessKeyId = accessKeyId;

        this.sshUser = sshUser;
        this.sshPrivateKey = sshPrivateKey;
    }

    public AWSConfig(URI endpoint, Region region, String secretAccessKey,
            String accessKeyId, String sshUser, String sshPrivateKey,
            AWSInstancesConfig awsInstancesConfig) {
        this(endpoint, region, secretAccessKey, accessKeyId, sshUser,
                sshPrivateKey);
        this.awsInstancesConfig = awsInstancesConfig;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public Region getRegion() {
        return region;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSshUser() {
        return sshUser;
    }

    public String getSshPrivateKey() {
        return sshPrivateKey;
    }

    public AWSInstancesConfig getAwsInstancesConfig() {
        return awsInstancesConfig;
    }

    public class AWSInstancesConfig {
        String amiId;
        InstanceType instanceType;
        String keyName;
        Collection<String> securityGroups;
        Collection<TagSpecification> tagSpecifications;
        Integer numInstances;

        public AWSInstancesConfig() {
        }

        public String getAmiId() {
            return amiId;
        }

        public void setAmiId(String amiId) {
            this.amiId = amiId;
        }

        public InstanceType getInstanceType() {
            return instanceType;
        }

        public void setInstanceType(InstanceType instanceType) {
            this.instanceType = instanceType;
        }

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }

        public Collection<String> getSecurityGroups() {
            return securityGroups;
        }

        public void setSecurityGroups(Collection<String> securityGroups) {
            this.securityGroups = securityGroups;
        }

        public Collection<TagSpecification> getTagSpecifications() {
            return tagSpecifications;
        }

        public void setTagSpecifications(
                Collection<TagSpecification> tagSpecifications) {
            this.tagSpecifications = tagSpecifications;
        }

        public Integer getNumInstances() {
            return numInstances;
        }

        public void setNumInstances(Integer numInstances) {
            this.numInstances = numInstances;
        }

        @Override
        public String toString() {
            return "AWSInstancesConfig [amiId=" + amiId + ", instanceType="
                    + instanceType + ", keyName=" + keyName
                    + ", securityGroups=" + securityGroups
                    + ", tagSpecifications=" + tagSpecifications
                    + ", numInstances=" + numInstances + "]";
        }
    }

    @Override
    public String toString() {
        return "AWSConfig [endpoint=" + endpoint + ", region=" + region
                + ", secretAccessKey=" + secretAccessKey + ", accessKeyId="
                + accessKeyId + ", sshUser=" + sshUser + ", sshPrivateKey="
                + sshPrivateKey + ", awsInstancesConfig=" + awsInstancesConfig
                + "]";
    }

}