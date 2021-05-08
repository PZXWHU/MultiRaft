package com.pzx.raft.kv.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RegionInfo {

    long leaderId;

    Region region;

}
