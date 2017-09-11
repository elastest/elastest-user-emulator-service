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

    public String owner;
    public List<Long> blockIds;
    public long creationTimeMs;
    public int inMemoryPercentage;
    public long lastModificationTimeMs;
    public String persistenceState;
    public boolean pinned;
    public boolean mountPoint;
    public List<FileBlockInfo> fileBlockInfos;
    public boolean completed;
    public boolean persisted;
    public boolean cacheable;
    public int ttl;
    public String ttlAction;
    public String group;
    public int mode;
    public long blockSizeBytes;
    public String ufsPath;
    public long mountId;
    public boolean folder;
    public long length;
    public String name;
    public String path;

    class FileBlockInfo {
        public List<Object> ufsLocations;
        public BlockInfo blockInfo;
        public long offset;
    }

    class BlockInfo {
        public long blockId;
        public List<Location> locations;
        public long length;
    }

    class Location {
        public long workerId;
        public WorkerAddress workerAddress;
        public String tierAlias;
    }

    class WorkerAddress {
        public String host;
        public int rpcPort;
        public String domainSocketPath;
        public int dataPort;
        public int webPort;
    }

}
