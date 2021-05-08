package com.pzx.raft.kv.service.entity;

import com.pzx.raft.kv.entity.Instruction;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StoreHeartbeatResponse {

    Instruction instruction;

}
