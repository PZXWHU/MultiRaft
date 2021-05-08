package com.pzx.raft.kv.service.entity;

import com.pzx.raft.kv.entity.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StoreHeartbeatRequest {

    private StoreInfo storeInfo;

    private StoreStats storeStats;

}
