package com.pzx.raft.kv;

import com.pzx.raft.kv.meta.RegionMeta;

public interface RouteTable {

    RegionMeta findRegionByKey(byte[] key);

}
