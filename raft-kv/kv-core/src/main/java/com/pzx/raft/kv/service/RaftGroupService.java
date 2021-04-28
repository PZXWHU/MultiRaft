package com.pzx.raft.kv.service;

import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.kv.service.entity.CreateRegionRequest;
import com.pzx.raft.kv.service.entity.CreateRegionResponse;

public interface RaftGroupService extends RaftConsensusService {

    CreateRegionResponse createRegion(CreateRegionRequest request);

}
