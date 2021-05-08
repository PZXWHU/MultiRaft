package com.pzx.raft.kv.service.entity;

import com.pzx.raft.kv.entity.Instruction;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RegionHeartbeatResponse {

    Instruction instruction;

}
