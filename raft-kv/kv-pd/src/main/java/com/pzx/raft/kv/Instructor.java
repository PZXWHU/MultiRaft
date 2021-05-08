package com.pzx.raft.kv;

import com.pzx.raft.kv.entity.Instruction;
import com.pzx.raft.kv.entity.RegionStats;
import com.pzx.raft.kv.entity.StoreStats;

public interface Instructor {

    Instruction instructStore(StoreStats storeStats);

    Instruction instructRegion(RegionStats regionStats);

}
