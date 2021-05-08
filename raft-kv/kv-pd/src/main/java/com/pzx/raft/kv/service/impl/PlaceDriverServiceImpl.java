package com.pzx.raft.kv.service.impl;

import com.pzx.raft.kv.Instructor;
import com.pzx.raft.kv.PlaceDriverServer;
import com.pzx.raft.kv.RegionRouteTable;
import com.pzx.raft.kv.entity.Instruction;
import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.StoreInfo;
import com.pzx.raft.kv.meta.MetaStorage;
import com.pzx.raft.kv.service.PlaceDriverService;
import com.pzx.raft.kv.service.entity.RegionHeartbeatRequest;
import com.pzx.raft.kv.service.entity.RegionHeartbeatResponse;
import com.pzx.raft.kv.service.entity.StoreHeartbeatRequest;
import com.pzx.raft.kv.service.entity.StoreHeartbeatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PlaceDriverServiceImpl implements PlaceDriverService {

    private MetaStorage metaStorage;

    private Instructor instructor;

    private RegionRouteTable regionRouteTable;

    public PlaceDriverServiceImpl(PlaceDriverServer placeDriverServer) {
        this.metaStorage = placeDriverServer.getMetaStorage();
        this.instructor = placeDriverServer.getInstructor();
        this.regionRouteTable = placeDriverServer.getRegionRouteTable();
    }

    @Override
    public StoreHeartbeatResponse storeHeartbeat(StoreHeartbeatRequest request) {
        log.info("收到store heartbeat ： " + request);
        if (request.getStoreInfo() != null)
            setStoreInfo(request.getStoreInfo());
        Instruction instruction = instructor.instructStore(request.getStoreStats());
        StoreHeartbeatResponse storeHeartbeatResponse = new StoreHeartbeatResponse();
        storeHeartbeatResponse.setInstruction(instruction);
        return storeHeartbeatResponse;
    }

    @Override
    public RegionHeartbeatResponse regionHeartbeat(RegionHeartbeatRequest request) {
        log.info("收到region heartbeat ： " + request);
        if (request.getRegionInfo() != null)
            setRegionInfo(request.getRegionInfo());
        Instruction instruction = instructor.instructRegion(request.getRegionStats());
        RegionHeartbeatResponse regionHeartbeatResponse = new RegionHeartbeatResponse();
        regionHeartbeatResponse.setInstruction(instruction);
        return regionHeartbeatResponse;
    }

    @Override
    public void setStoreInfo(StoreInfo storeInfo) {
        metaStorage.updateStoreInfo(storeInfo);
    }

    @Override
    public void setRegionInfo(RegionInfo regionInfo) {
        metaStorage.updateRegionInfo(regionInfo);
        regionRouteTable.addOrUpdateRegion(regionInfo.getRegion());
    }

    @Override
    public StoreInfo getStoreInfo(long storeId) {
        return metaStorage.getStoreInfo(storeId);
    }

    @Override
    public RegionInfo getRegionInfo(long regionId) { return metaStorage.getRegionInfo(regionId); }

    @Override
    public Long createRegionId() {
        long newRegionId = metaStorage.createRegionId();
        return newRegionId;
    }

    @Override
    public RegionInfo findRegionByKey(byte[] key) {
        return regionRouteTable.findRegionByKey(key);
    }

    @Override
    public List<RegionInfo> findRegionsByKeyRange(byte[] startKey, byte[] endKey) {
        return regionRouteTable.findRegionsByKeyRange(startKey, endKey);
    }
}
