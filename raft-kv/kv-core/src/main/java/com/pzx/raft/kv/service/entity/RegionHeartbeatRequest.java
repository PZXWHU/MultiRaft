package com.pzx.raft.kv.service.entity;

import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.RegionStats;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RegionHeartbeatRequest {

    private RegionInfo regionInfo;

    private RegionStats regionStats;

}
