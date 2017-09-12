/*
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.elastest.eus.edm;

import java.util.List;

/**
 * Class to parse list-files from Alluxio JSON response.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
public class EdmAluxioFile {

    private String owner;
    private List<Long> blockIds;
    private long creationTimeMs;
    private int inMemoryPercentage;
    private long lastModificationTimeMs;
    private String persistenceState;
    private boolean pinned;
    private boolean mountPoint;
    private List<FileBlockInfo> fileBlockInfos;
    private boolean completed;
    private boolean persisted;
    private boolean cacheable;
    private int ttl;
    private String ttlAction;
    private String group;
    private int mode;
    private long blockSizeBytes;
    private String ufsPath;
    private long mountId;
    private boolean folder;
    private long length;
    private String name;
    private String path;

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setBlockIds(List<Long> blockIds) {
        this.blockIds = blockIds;
    }

    public void setCreationTimeMs(long creationTimeMs) {
        this.creationTimeMs = creationTimeMs;
    }

    public void setInMemoryPercentage(int inMemoryPercentage) {
        this.inMemoryPercentage = inMemoryPercentage;
    }

    public void setLastModificationTimeMs(long lastModificationTimeMs) {
        this.lastModificationTimeMs = lastModificationTimeMs;
    }

    public void setPersistenceState(String persistenceState) {
        this.persistenceState = persistenceState;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void setMountPoint(boolean mountPoint) {
        this.mountPoint = mountPoint;
    }

    public void setFileBlockInfos(List<FileBlockInfo> fileBlockInfos) {
        this.fileBlockInfos = fileBlockInfos;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public void setTtlAction(String ttlAction) {
        this.ttlAction = ttlAction;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setBlockSizeBytes(long blockSizeBytes) {
        this.blockSizeBytes = blockSizeBytes;
    }

    public void setUfsPath(String ufsPath) {
        this.ufsPath = ufsPath;
    }

    public void setMountId(long mountId) {
        this.mountId = mountId;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOwner() {
        return owner;
    }

    public List<Long> getBlockIds() {
        return blockIds;
    }

    public long getCreationTimeMs() {
        return creationTimeMs;
    }

    public int getInMemoryPercentage() {
        return inMemoryPercentage;
    }

    public long getLastModificationTimeMs() {
        return lastModificationTimeMs;
    }

    public String getPersistenceState() {
        return persistenceState;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isMountPoint() {
        return mountPoint;
    }

    public List<FileBlockInfo> getFileBlockInfos() {
        return fileBlockInfos;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public int getTtl() {
        return ttl;
    }

    public String getTtlAction() {
        return ttlAction;
    }

    public String getGroup() {
        return group;
    }

    public int getMode() {
        return mode;
    }

    public long getBlockSizeBytes() {
        return blockSizeBytes;
    }

    public String getUfsPath() {
        return ufsPath;
    }

    public long getMountId() {
        return mountId;
    }

    public boolean isFolder() {
        return folder;
    }

    public long getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    class FileBlockInfo {
        private List<Object> ufsLocations;
        private BlockInfo blockInfo;
        private long offset;

        public List<Object> getUfsLocations() {
            return ufsLocations;
        }

        public void setUfsLocations(List<Object> ufsLocations) {
            this.ufsLocations = ufsLocations;
        }

        public BlockInfo getBlockInfo() {
            return blockInfo;
        }

        public void setBlockInfo(BlockInfo blockInfo) {
            this.blockInfo = blockInfo;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

    }

    class BlockInfo {
        private long blockId;
        private List<Location> locations;
        private long length;

        public long getBlockId() {
            return blockId;
        }

        public void setBlockId(long blockId) {
            this.blockId = blockId;
        }

        public List<Location> getLocations() {
            return locations;
        }

        public void setLocations(List<Location> locations) {
            this.locations = locations;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }

    }

    class Location {
        private long workerId;
        private WorkerAddress workerAddress;
        private String tierAlias;

        public long getWorkerId() {
            return workerId;
        }

        public void setWorkerId(long workerId) {
            this.workerId = workerId;
        }

        public WorkerAddress getWorkerAddress() {
            return workerAddress;
        }

        public void setWorkerAddress(WorkerAddress workerAddress) {
            this.workerAddress = workerAddress;
        }

        public String getTierAlias() {
            return tierAlias;
        }

        public void setTierAlias(String tierAlias) {
            this.tierAlias = tierAlias;
        }

    }

    class WorkerAddress {
        private String host;
        private int rpcPort;
        private String domainSocketPath;
        private int dataPort;
        private int webPort;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getRpcPort() {
            return rpcPort;
        }

        public void setRpcPort(int rpcPort) {
            this.rpcPort = rpcPort;
        }

        public String getDomainSocketPath() {
            return domainSocketPath;
        }

        public void setDomainSocketPath(String domainSocketPath) {
            this.domainSocketPath = domainSocketPath;
        }

        public int getDataPort() {
            return dataPort;
        }

        public void setDataPort(int dataPort) {
            this.dataPort = dataPort;
        }

        public int getWebPort() {
            return webPort;
        }

        public void setWebPort(int webPort) {
            this.webPort = webPort;
        }

    }

}
