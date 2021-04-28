package com.pzx.raft.kv.service.impl;

import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.core.service.entity.*;
import com.pzx.raft.core.service.impl.RaftConsensusServiceImpl;
import com.pzx.raft.kv.RegionEngine;
import com.pzx.raft.kv.StoreEngine;
import com.pzx.raft.kv.exception.RaftGroupServiceException;
import com.pzx.raft.kv.service.RaftGroupService;
import com.pzx.raft.kv.service.entity.CreateRegionRequest;
import com.pzx.raft.kv.service.entity.CreateRegionResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RaftGroupServiceImpl implements RaftGroupService {

    private StoreEngine storeEngine;

    private Map<Long, RaftConsensusService> consensusServiceMap = new ConcurrentHashMap<>();

    public RaftGroupServiceImpl(StoreEngine storeEngine) {
        this.storeEngine = storeEngine;
        for(Map.Entry<Long, RegionEngine> entry: storeEngine.getRegionEngineTable().entrySet()){
            consensusServiceMap.put(entry.getKey(), new RaftConsensusServiceImpl(entry.getValue().getRaftKVStore().getRaftNode()));
        }
    }

    @Override
    public ClusterMembershipChangeResponse addNode(ClusterMembershipChangeRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
        }
        return consensusServiceMap.get(request.getGroupId()).addNode(request);
    }

    @Override
    public ClusterMembershipChangeResponse removeNode(ClusterMembershipChangeRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
        }
        return consensusServiceMap.get(request.getGroupId()).removeNode(request);
    }

    @Override
    public ReqVoteResponse requestVote(ReqVoteRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
        }
        return consensusServiceMap.get(request.getGroupId()).requestVote(request);
    }

    @Override
    public AppendEntriesResponse appendEntries(AppendEntriesRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
        }
        return consensusServiceMap.get(request.getGroupId()).appendEntries(request);
    }

    @Override
    public InstallSnapshotResponse installSnapshot(InstallSnapshotRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
        }
        return consensusServiceMap.get(request.getGroupId()).installSnapshot(request);
    }

    @Override
    public CreateRegionResponse createRegion(CreateRegionRequest request) {
        CreateRegionResponse createRegionResponse = CreateRegionResponse.builder().build();
        if (consensusServiceMap.containsKey(request.getRegion().getRegionId())){
            createRegionResponse.setSuccess(false);
            createRegionResponse.setMessage("the region has already existed : " + request.getRegion());
        }else {
            RegionEngine regionEngine = storeEngine.createRegion(request.getRegion());
            consensusServiceMap.put(regionEngine.getRegion().getRegionId(), new RaftConsensusServiceImpl(regionEngine.getRaftKVStore().getRaftNode()));
            createRegionResponse.setSuccess(true);
        }

        return createRegionResponse;

    }

}
