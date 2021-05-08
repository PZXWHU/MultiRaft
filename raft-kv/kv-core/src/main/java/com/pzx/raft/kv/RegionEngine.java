package com.pzx.raft.kv;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import com.pzx.raft.core.RaftLogStorage;
import com.pzx.raft.core.RaftMetaStorage;
import com.pzx.raft.core.RaftStateMachine;
import com.pzx.raft.core.config.NodeConfig;
import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.core.entity.KVCommand;
import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.core.node.RaftNode;
import com.pzx.raft.core.storage.KVRaftLogStorage;
import com.pzx.raft.core.storage.KVRaftMetaStorage;
import com.pzx.raft.core.storage.KVRaftStateMachine;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.kv.entity.*;
import com.pzx.raft.kv.exception.RegionException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
@Setter
@Slf4j
public class RegionEngine implements PerKVStore, AsyncKVStore {

    private Region region;

    //private RaftKVStore raftKVStore;
    private RaftNode raftNode;

    private KVRaftStateMachine raftStateMachine;

    private StoreEngine storeEngine;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private Lock readLock = lock.readLock();

    private Lock writeLock = lock.writeLock();

    private Lock splitLock  = new ReentrantLock();




    public RegionEngine(Region region, StoreEngine storeEngine) {
        this.region = region;
        this.storeEngine = storeEngine;
        this.raftNode = createRaftNode();
        this.raftStateMachine = (KVRaftStateMachine) raftNode.getRaftStateMachine();
        //avoid raft group member be changed from outside
        //region.setRaftGroupAddress(Collections.unmodifiableMap(region.getRaftGroupAddress()));

    }

    //启动raftNode
    public void start(){

        raftNode.start();
    }
    public void stop(){

        raftNode.stop();
    }

    private RaftNode createRaftNode(){
        RaftConfig raftConfig = new RaftConfig();
        raftConfig.setRaftGroupAddress(region.getRaftGroupAddress());

        RaftLogStorage raftLogStorage = new KVRaftLogStorage(region.getRegionId(), storeEngine.getPerKVStore());
        RaftMetaStorage raftMetaStorage = new KVRaftMetaStorage(region.getRegionId(), storeEngine.getPerKVStore());
        RaftStateMachine raftStateMachine = new RegionStateMachine(region, storeEngine.getMemKVStore());

        NodeConfig nodeConfig = NodeConfig.builder()
                .serverId(storeEngine.getStoreId())
                .groupId(region.getRegionId())
                .nodeHome(storeEngine.getStoreConfig().getStoreHome())
                .raftConfig(raftConfig)
                .raftLogStorage(raftLogStorage)
                .raftMetaStorage(raftMetaStorage)
                .raftStateMachine(raftStateMachine)
                .build();
        RaftNode raftNode = new RaftNode(nodeConfig);

        raftNode.addUserDefinedCommandListener(new SplitRegionCommandListener(this));
        raftNode.addUserDefinedCommandListener(new EpochSMCommandListener(this));
        return raftNode;
    }

    public boolean isLeader(){
        /*return raftKVStore.getRaftNode().isLeader();*/
        return raftNode.isLeader();
    }


    /**
     * 获取范围内的估计数量大小，然后scan一半
     * @return
     */
    public byte[] getSplitKey(){
        long approximateKVNum = raftStateMachine.getKvPrefixAdapter().getApproximateKeysInRange(region.getStartKey(), region.getEndKey());
        return raftStateMachine.getKvPrefixAdapter().jumpOver(region.getStartKey(), approximateKVNum / 2);
    }

    public RegionInfo collectRegionInfo(){
        RegionInfo regionInfo = new RegionInfo();
        regionInfo.setLeaderId(raftNode.getLeaderId());
        regionInfo.setRegion(region);
        return regionInfo;
    }


    public RegionStats collectRegionStats(){
        RegionStats regionStats = new RegionStats();
        regionStats.setRegionId(region.getRegionId());
        long approximateKVNum = raftStateMachine.getKvPrefixAdapter().getApproximateKeysInRange(region.getStartKey(), region.getEndKey());
        regionStats.setApproximateNum(approximateKVNum);
        return regionStats;
    }


    @Override
    public CompletableFuture<byte[]> getAsync(byte[] key) {
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        if (isSplitting()){
            completableFuture.completeExceptionally(new RegionException("ths region is splitting"));
        }else {
            completableFuture.complete(raftStateMachine.get(key));
        }
        return completableFuture;
    }

    @Override
    public CompletableFuture<Boolean> putAsync(byte[] key, byte[] value) {
        EpochSMCommand epochSMCommand = new EpochSMCommand();
        epochSMCommand.setKey(key);
        epochSMCommand.setValue(value);
        epochSMCommand.setRegionEpoch(region.getRegionEpoch());
        CompletableFuture<Boolean> completableFuture = raftNode.replicateEntry(epochSMCommand);
        return completableFuture;
    }

    @Override
    public CompletableFuture<Boolean> deleteAsync(byte[] key) {
        return putAsync(key, null);
    }

    @Override
    public CompletableFuture<List<byte[]>> scanAsync(byte[] startKey, byte[] endKey) {
        CompletableFuture<List<byte[]>> completableFuture = new CompletableFuture<>();
        if (isSplitting()){
            completableFuture.completeExceptionally(new RegionException("ths region is splitting"));
        }else {
            completableFuture.complete(raftStateMachine.scan(startKey, endKey));
        }
        return completableFuture;
    }


    public void split(long regionEpoch){
        if (!splitLock.tryLock()) {
            return;
        }
        try {
            if (isSplitting()){
                log.warn("the regionEngine is splitting " , region.getRegionId());
                return;
            }
            if (regionEpoch < region.getRegionEpoch()){
                log.warn("the regionEngine {} is already split " , region.getRegionId());
                return;
            }
            logger.info("regionEngine {} start splitting ",  region.getRegionId());

            long newRegionId = storeEngine.getPdClient().createRegionId();
            byte[] splitKey = getSplitKey();

            Region newRegion = getRegion().copy();
            Region oldRegion = getRegion().copy();

            newRegion.setRegionId(newRegionId);
            newRegion.setStartKey(splitKey);
            newRegion.setRegionEpoch(1);

            oldRegion.setEndKey(splitKey);
            oldRegion.setRegionEpoch(oldRegion.getRegionEpoch() + 1);

            //在分裂日志写入的时候要记录lastSplitIndex！！ 以此判断是否正在分裂过程中

            raftNode.replicateEntry(new SplitRegionCommand(oldRegion, newRegion));
        }finally {
            splitLock.unlock();
        }

    }

    public boolean isSplitting(){
        byte[] lastSplitIndex = raftNode.getRaftMetaStorage().getUserData(SplitRegionCommandListener.lastSplitIndexKey);

        //System.out.println(ByteUtils.bytesToLong(lastSplitIndex) + "  " +  raftNode.getLastAppliedIndex());
        if (lastSplitIndex != null && ByteUtils.bytesToLong(lastSplitIndex) > raftNode.getLastAppliedIndex())
            return true;
        else
            return false;
    }

}
