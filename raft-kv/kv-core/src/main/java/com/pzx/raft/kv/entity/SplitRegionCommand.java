package com.pzx.raft.kv.entity;

import com.pzx.raft.core.entity.UserDefinedCommand;
import lombok.Getter;

@Getter
public class SplitRegionCommand implements UserDefinedCommand {

    private Region oldRegion;

    private Region newRegion;

    public SplitRegionCommand(Region oldRegion, Region newRegion){
        this.oldRegion = oldRegion;
        this.newRegion = newRegion;
    }


}
