package com.pzx.raft.kv.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class StoreStats {

    long regionSize;

    long regionLeaderNum;

}
