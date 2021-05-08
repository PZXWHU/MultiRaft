package com.pzx.raft.kv.service;

import com.pzx.raft.kv.service.entity.MultiRaftKVClientRequest;
import com.pzx.raft.kv.service.entity.MultiRaftKVClientResponse;

public interface MultiRaftKVClientService {

    MultiRaftKVClientResponse operateKV(MultiRaftKVClientRequest request);

}
