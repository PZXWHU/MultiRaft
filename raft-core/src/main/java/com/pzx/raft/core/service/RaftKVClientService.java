package com.pzx.raft.core.service;

import com.pzx.raft.core.service.entity.ClientKVRequest;
import com.pzx.raft.core.service.entity.ClientKVResponse;

public interface RaftKVClientService {
    ClientKVResponse operateKV(ClientKVRequest request);
}
