package com.pzx.raft.kv.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
@ToString
public class StoreInfo {

    long storeId;

    Set<Long> regionIds;

}
