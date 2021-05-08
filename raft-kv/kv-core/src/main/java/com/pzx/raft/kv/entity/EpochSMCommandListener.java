package com.pzx.raft.kv.entity;

import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.core.entity.UserDefinedCommand;
import com.pzx.raft.core.entity.UserDefinedCommandListener;
import com.pzx.raft.kv.RegionEngine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpochSMCommandListener implements UserDefinedCommandListener {

    private RegionEngine regionEngine;

    public EpochSMCommandListener(RegionEngine regionEngine) {
        this.regionEngine = regionEngine;
    }

    @Override
    public void onApply(UserDefinedCommand userDefinedCommand) {
        if (userDefinedCommand instanceof EpochSMCommand){
            EpochSMCommand epochSMCommand = (EpochSMCommand)userDefinedCommand;
            if (regionEngine.getRegion().getRegionEpoch() == epochSMCommand.getRegionEpoch()){
                SMCommand smCommand = new SMCommand(epochSMCommand.getKey(), epochSMCommand.getValue());
                regionEngine.getRaftNode().applyCommand(smCommand);
            }else{
                log.warn("Command has expired regionEpoch, so skip applying this Command :" + epochSMCommand);
            }
        }
    }

    @Override
    public void onWrite(UserDefinedCommand userDefinedCommand, long index, long term) {

    }
}
