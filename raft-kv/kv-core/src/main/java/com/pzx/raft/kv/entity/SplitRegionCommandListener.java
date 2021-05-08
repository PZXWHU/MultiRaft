package com.pzx.raft.kv.entity;

import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.core.entity.UserDefinedCommand;
import com.pzx.raft.core.entity.UserDefinedCommandListener;
import com.pzx.raft.core.node.RaftNode;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.kv.RegionEngine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SplitRegionCommandListener implements UserDefinedCommandListener {

    private RegionEngine regionEngine;

    public static final byte[] lastSplitIndexKey =  ByteUtils.stringToBytes("last_split_idx");

    public SplitRegionCommandListener(RegionEngine regionEngine) {
        this.regionEngine = regionEngine;
    }

    @Override
    public void onApply(UserDefinedCommand userDefinedCommand) {
        if(userDefinedCommand instanceof SplitRegionCommand){
            SplitRegionCommand splitRegionCommand = (SplitRegionCommand)userDefinedCommand;
            Region oldRegion = splitRegionCommand.getOldRegion();
            Region newRegion = splitRegionCommand.getNewRegion();

            Region updateOldRegion = regionEngine.getRegion();

            if (oldRegion.getRegionEpoch() > updateOldRegion.getRegionEpoch()){
                updateOldRegion.setEndKey(oldRegion.getEndKey());
                updateOldRegion.setRegionEpoch(oldRegion.getRegionEpoch());
            }

            //必须要考虑幂等性，因为日志是可能会被重复apply的
            // （宕机后重启，而store可以获取pd上的信息，从而已经获取分裂后的region信息）
            if (!regionEngine.getStoreEngine().getRegionEngineTable().containsKey(newRegion.getRegionId())){
                RegionEngine newRegionEngine = regionEngine.getStoreEngine().createRegionEngine(newRegion, false);
                RaftNode raftNode = newRegionEngine.getRaftNode();
                raftNode.takeSnapshot();
                newRegionEngine.start();
                log.info("region {} finish splitting, create new Region {}", oldRegion.getRegionId(), newRegion.getRegionId());
            }
        }
    }

    @Override
    public void onWrite(UserDefinedCommand userDefinedCommand, long index, long term) {
        regionEngine.getRaftNode().getRaftMetaStorage().setUserData(lastSplitIndexKey, ByteUtils.longToBytes(index));
    }
}
