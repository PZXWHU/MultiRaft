package com.pzx.raft.kv.service;

import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.StoreInfo;
import com.pzx.raft.kv.service.entity.RegionHeartbeatRequest;
import com.pzx.raft.kv.service.entity.RegionHeartbeatResponse;
import com.pzx.raft.kv.service.entity.StoreHeartbeatRequest;
import com.pzx.raft.kv.service.entity.StoreHeartbeatResponse;

import java.util.List;

public interface PlaceDriverService {

    StoreHeartbeatResponse storeHeartbeat(StoreHeartbeatRequest request);

    RegionHeartbeatResponse regionHeartbeat(RegionHeartbeatRequest request);

    StoreInfo getStoreInfo(long storeId);

    void setStoreInfo(StoreInfo storeInfo);

    RegionInfo getRegionInfo(long regionId);

    void setRegionInfo(RegionInfo regionInfo);

    Long createRegionId();

    RegionInfo findRegionByKey(byte[] key);

    List<RegionInfo> findRegionsByKeyRange(byte[] startKey, byte[] endKey);

}

