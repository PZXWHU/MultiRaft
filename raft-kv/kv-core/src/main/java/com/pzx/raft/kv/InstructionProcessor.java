package com.pzx.raft.kv;

import com.pzx.raft.kv.entity.Instruction;
import com.pzx.raft.kv.entity.RegionInfo;

public class InstructionProcessor {

    private StoreEngine storeEngine;

    public InstructionProcessor(StoreEngine storeEngine) {
        this.storeEngine = storeEngine;
    }

    public void processInstruction(Instruction instruction){
        switch (instruction.getType()){
            case SPLIT:
                split(instruction);
        }
    }

    private void split(Instruction instruction){

        RegionInfo regionInfo = (RegionInfo)instruction.getData();
        storeEngine.splitRegion(regionInfo.getRegion().getRegionId(), regionInfo.getRegion().getRegionEpoch());
    }

}
