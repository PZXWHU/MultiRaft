package com.pzx.raft.core.service;

import com.pzx.raft.core.service.entity.KVClientRequest;
import com.pzx.raft.core.service.entity.KVClientResponse;

public interface RaftKVClientService {
    KVClientResponse operateKV(KVClientRequest request);
}
