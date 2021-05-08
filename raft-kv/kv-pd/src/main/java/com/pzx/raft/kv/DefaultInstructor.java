package com.pzx.raft.kv;

import com.pzx.raft.kv.entity.Instruction;
import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.RegionStats;
import com.pzx.raft.kv.entity.StoreStats;

public class DefaultInstructor implements Instructor {

    private PlaceDriverServer placeDriverServer;

    public DefaultInstructor(PlaceDriverServer placeDriverServer) {
        this.placeDriverServer = placeDriverServer;
    }

    @Override
    public Instruction instructStore(StoreStats storeStats) {
        return null;
    }

    @Override
    public Instruction instructRegion(RegionStats regionStats) {
        Instruction instruction = new Instruction();
        if (regionStats.getApproximateNum() > placeDriverServer.getPdServerConfig().getMaxSizePerRegion()){
            instruction.setType(Instruction.InstructionType.SPLIT);
            long regionId = regionStats.getRegionId();
            RegionInfo regionInfo = placeDriverServer.getMetaStorage().getRegionInfo(regionId);
            instruction.setData(regionInfo);
        }
        else
            instruction.setType(Instruction.InstructionType.EMPTY);
        return instruction;
    }
}
