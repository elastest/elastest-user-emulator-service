package io.elastest.eus.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AWSConfig {
    Region region;
    String secretAccessKey;
    String accessKeyId;

    String sshUser;
    String sshPrivateKey;

    AWSInstancesConfig awsInstancesConfig;

    public AWSConfig() {
        // Empty default construct
    }

    public AWSConfig(Region region, String secretAccessKey, String accessKeyId,
            String sshUser, String sshPrivateKey) {
        super();
        this.region = region;
        this.secretAccessKey = secretAccessKey;
        this.accessKeyId = accessKeyId;

        this.sshUser = sshUser;
        this.sshPrivateKey = sshPrivateKey;
    }

    public AWSConfig(Region region, String secretAccessKey, String accessKeyId,
            String sshUser, String sshPrivateKey,
            AWSInstancesConfig awsInstancesConfig) {
        this(region, secretAccessKey, accessKeyId, sshUser, sshPrivateKey);
        this.awsInstancesConfig = awsInstancesConfig;
    }

    public AWSConfig(String region, String secretAccessKey, String accessKeyId,
            String sshUser, String sshPrivateKey) {
        this(Region.of(region), secretAccessKey, accessKeyId, sshUser,
                sshPrivateKey);
    }

    public AWSConfig(String region, String secretAccessKey, String accessKeyId,
            String sshUser, String sshPrivateKey,
            AWSInstancesConfig awsInstancesConfig) {
        this(Region.of(region), secretAccessKey, accessKeyId, sshUser,
                sshPrivateKey, awsInstancesConfig);
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setRegion(String region) {
        this.setRegion(Region.of(region));
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

    @JsonIgnoreProperties(ignoreUnknown = true)
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

        public void setInstanceType(String instanceType) {
            this.setInstanceType(InstanceType.fromValue(instanceType));
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

        // public void setTagSpecifications(
        // Collection<TagSpecification> tagSpecifications) {
        // this.tagSpecifications = tagSpecifications;
        // }
        //

        public void setTagSpecifications(
                List<Map<String, Object>> tagSpecifications) {
            if (tagSpecifications != null) {
                this.tagSpecifications = new ArrayList<>();
                for (Map<String, Object> tagSpec : tagSpecifications) {
                    if (tagSpec != null) {
                        String resourceType = (String) tagSpec
                                .get("resourceType");
                        List<Tag> tags = new ArrayList<>();
                        if (tagSpec.get("tags") != null) {
                            for (Map<String, String> tag : (List<Map<String, String>>) tagSpec
                                    .get("tags")) {
                                tags.add(Tag.builder().key(tag.get("key"))
                                        .value(tag.get("value")).build());
                            }
                        }
                        this.tagSpecifications.add(TagSpecification.builder()
                                .resourceType(resourceType).tags(tags).build());
                    }
                }
            }
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
        return "AWSConfig [region=" + region + ", secretAccessKey="
                + secretAccessKey + ", accessKeyId=" + accessKeyId
                + ", sshUser=" + sshUser + ", sshPrivateKey=" + sshPrivateKey
                + ", awsInstancesConfig=" + awsInstancesConfig + "]";
    }

}