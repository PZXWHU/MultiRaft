package com.pzx.raft.kv;

import com.pzx.raft.core.RaftLogStorage;
import com.pzx.raft.core.RaftMetaStorage;
import com.pzx.raft.core.RaftStateMachine;
import com.pzx.raft.core.config.NodeConfig;
import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.core.node.RaftNode;
import com.pzx.raft.core.storage.KVRaftLogStorage;
import com.pzx.raft.core.storage.KVRaftMetaStorage;
import com.pzx.raft.kv.entity.Region;
import lombok.Getter;

@Getter
public class RegionEngine {

    private Region region;

    private RaftKVStore raftKVStore;

    private StoreEngine storeEngine;


    public RegionEngine(Region region, StoreEngine storeEngine) {
        this.region = region;
        this.storeEngine = storeEngine;
        this.raftKVStore = new RaftKVStore(createRaftNode());
    }


    private RaftNode createRaftNode(){
        RaftConfig raftConfig = new RaftConfig();
        raftConfig.setRaftGroupAddress(region.getRaftGroupAddress());

        RaftLogStorage raftLogStorage = new KVRaftLogStorage(region.getRegionId(), storeEngine.getPerKVStore());
        RaftMetaStorage raftMetaStorage = new KVRaftMetaStorage(region.getRegionId(), storeEngine.getPerKVStore());
        RaftStateMachine raftStateMachine = new RegionStateMachine(region, storeEngine.getMemKVStore());

        NodeConfig nodeConfig = NodeConfig.builder()
                .serverId(storeEngine.getStoreId())
                .groupId(region.getRegionId())
                .nodeHome(storeEngine.getStoreConfig().getStoreHome())
                .raftConfig(raftConfig)
                .raftLogStorage(raftLogStorage)
                .raftMetaStorage(raftMetaStorage)
                .raftStateMachine(raftStateMachine)
                .build();
        RaftNode raftNode = new RaftNode(nodeConfig);
        raftNode.start();
        return raftNode;
    }

    public boolean isLeader(){
        return raftKVStore.getRaftNode().isLeader();
    }

}
