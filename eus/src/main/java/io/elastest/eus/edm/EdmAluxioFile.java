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

    String owner;
    List<Long> blockIds;
    long creationTimeMs;
    int inMemoryPercentage;
    long lastModificationTimeMs;
    String persistenceState;
    boolean pinned;
    boolean mountPoint;
    List<FileBlockInfo> fileBlockInfos;
    boolean completed;
    boolean persisted;
    boolean cacheable;
    int ttl;
    String ttlAction;
    String group;
    int mode;
    long blockSizeBytes;
    String ufsPath;
    long mountId;
    boolean folder;
    long length;
    String name;
    String path;

    public String getName() {
        return name;
    }

    class FileBlockInfo {
        List<Object> ufsLocations;
        BlockInfo blockInfo;
        long offset;
    }

    class BlockInfo {
        long blockId;
        List<Location> locations;
        long length;
    }

    class Location {
        long workerId;
        WorkerAddress workerAddress;
        String tierAlias;
    }

    class WorkerAddress {
        String host;
        int rpcPort;
        String domainSocketPath;
        int dataPort;
        int webPort;
    }

}
