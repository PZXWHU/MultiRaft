package com.pzx.raft.service;

import com.pzx.raft.KVDataBase;
import com.pzx.raft.service.entity.ClientKVRequest;
import com.pzx.raft.service.entity.ClientKVResponse;

public interface RaftKVClientService {
    ClientKVResponse operateKV(ClientKVRequest request);
}
