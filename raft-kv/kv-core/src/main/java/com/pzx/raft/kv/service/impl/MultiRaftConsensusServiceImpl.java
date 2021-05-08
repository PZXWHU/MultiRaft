package com.pzx.raft.kv.service.impl;

import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.core.service.entity.*;
import com.pzx.raft.kv.StoreEngine;
import com.pzx.raft.kv.service.MultiRaftConsensusService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MultiRaftConsensusServiceImpl implements MultiRaftConsensusService {

    private StoreEngine storeEngine;

    private Map<Long, RaftConsensusService> consensusServiceMap;

    public MultiRaftConsensusServiceImpl(StoreEngine storeEngine) {
        this.storeEngine = storeEngine;
        this.consensusServiceMap = storeEngine.getConsensusServiceMap();
    }

    /*private Region getLegalRegion(long regionId){
        RegionInfo regionInfo = storeEngine.getPdClient().getRegionInfo(regionId);
        if (regionInfo != null
                && regionInfo.getRegion().getRaftGroupAddress().values().contains(storeEngine.getServerAddress()))
            return regionInfo.getRegion();
        else
            return null;

    }*/

    @Override
    public ClusterMembershipChangeResponse addNode(ClusterMembershipChangeRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            //storeEngine.createRegionEngine(storeEngine.getPdClient().getRegionInfo(request.getGroupId()).getRegion(),true);
            //throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
            /*Region region = getLegalRegion(request.getGroupId());
            if (region != null)
                storeEngine.createRegionEngine(region, true);
            else {
                log.warn(" raft group {} is illegal for store {} :  ", request.getGroupId(), storeEngine.getStoreId());
                return ClusterMembershipChangeResponse.builder().build();
            }*/
            log.warn(" raft group {} is not found   ", request.getGroupId());
            return ClusterMembershipChangeResponse.builder().build();

        }
        return consensusServiceMap.get(request.getGroupId()).addNode(request);
    }

    @Override
    public ClusterMembershipChangeResponse removeNode(ClusterMembershipChangeRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            //throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
            /*Region region = getLegalRegion(request.getGroupId());
            if (region != null)
                storeEngine.createRegionEngine(region, true);
            else {
                log.warn(" raft group {} is illegal for store {} :  ", request.getGroupId(), storeEngine.getStoreId());
                return ClusterMembershipChangeResponse.builder().build();
            }*/
            log.warn(" raft group {} is not found   ", request.getGroupId());
            return ClusterMembershipChangeResponse.builder().build();
        }
        return consensusServiceMap.get(request.getGroupId()).removeNode(request);
    }

    @Override
    public ReqVoteResponse requestVote(ReqVoteRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            //storeEngine.createRegionEngine(storeEngine.getPdClient().getRegionInfo(request.getGroupId()).getRegion(),true);
            //throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
            /*Region region = getLegalRegion(request.getGroupId());
            if (region != null)
                storeEngine.createRegionEngine(region, true);
            else {
                log.warn(" raft group {} is illegal for store {} :  ", request.getGroupId(), storeEngine.getStoreId());
                return ReqVoteResponse.builder().build();
            }*/
            log.warn(" raft group {} is not found   ", request.getGroupId());
            return ReqVoteResponse.builder().build();

        }
        return consensusServiceMap.get(request.getGroupId()).requestVote(request);
    }

    @Override
    public AppendEntriesResponse appendEntries(AppendEntriesRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            //storeEngine.createRegionEngine(storeEngine.getPdClient().getRegionInfo(request.getGroupId()).getRegion(),true);
            //throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
            /*Region region = getLegalRegion(request.getGroupId());
            if (region != null)
                storeEngine.createRegionEngine(region, true);
            else {
                log.warn(" raft group {} is illegal for store {} :  ", request.getGroupId(), storeEngine.getStoreId());
                return AppendEntriesResponse.builder().build();
            }*/
            log.warn(" raft group {} is not found   ", request.getGroupId());
            return AppendEntriesResponse.builder().build();

        }
        return consensusServiceMap.get(request.getGroupId()).appendEntries(request);
    }

    @Override
    public InstallSnapshotResponse installSnapshot(InstallSnapshotRequest request) {
        if(!consensusServiceMap.containsKey(request.getGroupId())){
            //storeEngine.createRegionEngine(storeEngine.getPdClient().getRegionInfo(request.getGroupId()).getRegion(),true);
            //throw new RaftGroupServiceException("can not find raft group :  " + request.getGroupId());
            /*Region region = getLegalRegion(request.getGroupId());
            if (region != null)
                storeEngine.createRegionEngine(region, true);
            else {
                log.warn(" raft group {} is illegal for store {} :  ", request.getGroupId(), storeEngine.getStoreId());
                return InstallSnapshotResponse.builder().build();
            }*/
            log.warn(" raft group {} is not found   ", request.getGroupId());
            return InstallSnapshotResponse.builder().build();

        }
        return consensusServiceMap.get(request.getGroupId()).installSnapshot(request);
    }


}
