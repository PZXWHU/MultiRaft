package com.pzx.raft.kv.service.entity;

import com.pzx.raft.core.service.entity.KVClientRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter

public class MultiRaftKVClientRequest extends KVClientRequest {

    long regionId;

    long regionEpoch;

}
