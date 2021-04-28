package com.pzx.raft.kv.service;

import com.pzx.raft.kv.service.entity.RegionHeartbeatRequest;
import com.pzx.raft.kv.service.entity.RegionHeartbeatResponse;
import com.pzx.raft.kv.service.entity.StoreHeartbeatRequest;
import com.pzx.raft.kv.service.entity.StoreHeartbeatResponse;

public interface PlaceDriverService {

    StoreHeartbeatResponse storeHeartbeat(final StoreHeartbeatRequest request);

    RegionHeartbeatResponse regionHeartbeat(final RegionHeartbeatRequest request);

}
