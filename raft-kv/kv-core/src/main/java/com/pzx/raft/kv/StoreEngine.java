package com.pzx.raft.kv;

import com.pzx.raft.core.RaftLogStorage;
import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.core.service.RaftKVClientService;
import com.pzx.raft.core.service.impl.RaftConsensusServiceImpl;
import com.pzx.raft.core.service.impl.RaftKVClientServiceImpl;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.kv.config.PdClientConfig;
import com.pzx.raft.kv.config.StoreConfig;
import com.pzx.raft.kv.entity.*;
import com.pzx.raft.kv.service.entity.RegionHeartbeatRequest;
import com.pzx.raft.kv.service.entity.RegionHeartbeatResponse;
import com.pzx.raft.kv.service.entity.StoreHeartbeatRequest;
import com.pzx.raft.kv.service.entity.StoreHeartbeatResponse;
import com.pzx.raft.kv.service.impl.MultiRaftConsensusServiceImpl;
import com.pzx.raft.kv.service.impl.MultiRaftKVClientServiceImpl;
import com.pzx.rpc.invoke.RpcResponseCallBack;
import com.pzx.rpc.transport.RpcServer;
import com.pzx.rpc.transport.netty.server.NettyServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class StoreEngine {

    private long storeId;

    private String serverAddress;

    private RpcServer rpcServer;

    private PerKVStore perKVStore;

    private KVStore memKVStore;

    private StoreConfig storeConfig;

    private PdClientConfig pdClientConfig;

    private PdClient pdClient;

    private InstructionProcessor instructionProcessor;

    private ExecutorService executorService;

    //StoreEngine触发心跳的scheduled线程池
    private ScheduledExecutorService scheduledExecutorService;

    //表示选举过程的ScheduledFuture
    private ScheduledFuture storeHeartbeatScheduledFuture;

    //表示heartbeat的ScheduledFuture
    private ScheduledFuture regionHeartbeatScheduledFuture;

    private ConcurrentMap<Long, RegionEngine> regionEngineTable  = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Long, RaftConsensusService> consensusServiceMap = new ConcurrentHashMap<>();


    public StoreEngine(StoreConfig storeConfig, PdClientConfig pdClientConfig) {
        this.storeConfig = storeConfig;
        this.storeId = storeConfig.getStoreId();
        this.serverAddress = storeConfig.getServerAddress();
        this.perKVStore = new RocksKVStore(storeConfig.getStoreHome() + File.separator + "M_L");
        this.memKVStore = new RocksKVStore(storeConfig.getStoreHome() + File.separator + "FSM");
        this.pdClient = new PdClient(pdClientConfig);
        this.instructionProcessor = new InstructionProcessor(this);
        this.executorService = Executors.newFixedThreadPool(storeConfig.getExecutorThreadNum());
        this.scheduledExecutorService = Executors.newScheduledThreadPool(2);

        initAllRegion();
        initRpcServer();
    }

    private void initRpcServer(){
        String[] storeAddress = serverAddress.split(":");
        rpcServer = NettyServer.builder()
                .autoScanService(false)
                .serverAddress(new InetSocketAddress(storeAddress[0], Integer.valueOf(storeAddress[1])))
                .build();
        rpcServer.publishService(new MultiRaftConsensusServiceImpl(this));
        rpcServer.publishService(new MultiRaftKVClientServiceImpl(this));
    }

    public void start(){
        rpcServer.start();
        startStoreHeartbeat();
        startRegionHeartbeat();

    }

    public void stop(){
        rpcServer.stop();
        for(RegionEngine regionEngine : regionEngineTable.values())
            regionEngine.stop();
        if (storeHeartbeatScheduledFuture != null && storeHeartbeatScheduledFuture.isDone())
            storeHeartbeatScheduledFuture.cancel(true);
        if (regionHeartbeatScheduledFuture != null && regionHeartbeatScheduledFuture.isDone())
            regionHeartbeatScheduledFuture.cancel(true);
        executorService.shutdown();
        scheduledExecutorService.shutdown();

    }

    private void startStoreHeartbeat(){
        storeHeartbeatScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(()-> {
            StoreHeartbeatRequest storeHeartbeatRequest = new StoreHeartbeatRequest();
            storeHeartbeatRequest.setStoreInfo(collectStoreInfo());
            storeHeartbeatRequest.setStoreStats(collectStoreStats());
            pdClient.sendStoreHeartbeat(storeHeartbeatRequest, new HeartbeatResponseCallBack(instructionProcessor));
        },
         storeConfig.getStoreHeartbeatPeriodMilliseconds(),
         storeConfig.getStoreHeartbeatPeriodMilliseconds(),
         TimeUnit.MILLISECONDS);
    }

    private StoreInfo collectStoreInfo(){
        StoreInfo storeInfo = new StoreInfo();
        storeInfo.setStoreId(storeId);
        storeInfo.setRegionIds(regionEngineTable.keySet());
        return storeInfo;
    }

    private StoreStats collectStoreStats(){
        StoreStats storeStats = new StoreStats();
        storeStats.setRegionSize(regionEngineTable.size());
        long num = 0;
        for(RegionEngine engine : regionEngineTable.values()){
            if (engine.isLeader()) num++;
        }
        storeStats.setRegionLeaderNum(num);
        return storeStats;
    }

    private void startRegionHeartbeat(){
        regionHeartbeatScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(()->{
            for(RegionEngine regionEngine : regionEngineTable.values()){
                if (regionEngine.isLeader()){
                    RegionHeartbeatRequest regionHeartbeatRequest = new RegionHeartbeatRequest();
                    regionHeartbeatRequest.setRegionInfo(regionEngine.collectRegionInfo());
                    regionHeartbeatRequest.setRegionStats(regionEngine.collectRegionStats());
                    //System.out.println("发送region heartbeat ： " + regionHeartbeatRequest);
                    pdClient.sendRegionHeartbeat(regionHeartbeatRequest, new HeartbeatResponseCallBack(instructionProcessor));
                }
            }
        },
        storeConfig.getRegionHeartbeatPeriodMilliseconds(),
        storeConfig.getRegionHeartbeatPeriodMilliseconds(),
        TimeUnit.MILLISECONDS);
    }

    private class HeartbeatResponseCallBack implements RpcResponseCallBack{

        private InstructionProcessor instructionProcessor;

        public HeartbeatResponseCallBack(InstructionProcessor instructionProcessor) {
            this.instructionProcessor = instructionProcessor;
        }

        @Override
        public void onResponse(Object data) {
            Instruction instruction = null;
            if (data instanceof StoreHeartbeatResponse)
                instruction = ((StoreHeartbeatResponse) data).getInstruction();
            else if (data instanceof RegionHeartbeatResponse)
                instruction = ((RegionHeartbeatResponse) data).getInstruction();
            if (instruction != null)
                instructionProcessor.processInstruction(instruction);
        }

        @Override
        public void onException(Throwable throwable) {
            log.warn(throwable.getMessage());
        }
    }

    private void initAllRegion(){
        StoreInfo storeInfo = pdClient.getStoreInfo(storeId);
        for(long regionId : storeInfo.getRegionIds()){
            Region region = pdClient.getRegionInfo(regionId).getRegion();
            createRegionEngine(region, true);
        }
    }

    /**
     * create RegionEngine
     * @param region
     * @return
     */
   public RegionEngine createRegionEngine(Region region, boolean start){
       if (regionEngineTable.containsKey(region.getRegionId())){
           log.warn( "the regionEngine has already existed :" + region);
           return regionEngineTable.get(region.getRegionId());

       }
        RegionEngine regionEngine = new RegionEngine(region, this);
        regionEngineTable.put(region.getRegionId(), regionEngine);
        consensusServiceMap.put(region.getRegionId(), new RaftConsensusServiceImpl(regionEngine.getRaftNode()));
        log.info("create new regionEngine :" + region);
        if (start)
            regionEngine.start();
        return regionEngine;
    }


    /**
     * 分裂的实现：
     * 分裂暂时不考虑日志和状态机的分裂
     *
     * 日志：对于非幂等性的状态机，已经applied的日志不会再次apply，所以分裂的oldRegion保留旧日志不会有问题，因为不会再次apply
     *      对于幂等性的状态机，由于我们采用的是多个region的状态机公用一个底层存储，所以分裂的region保留旧日志不会有问题，因为多次apply无影响
     *      分裂的oldRegion保留旧日志会在takesnapshot后消除。
     *      分裂的newRegion必须立刻takesnapshot，因为如果oldregion takesnapshot后，就不存在newRegion中数据的持久化日志了，可能会造成数据丢失
     *
     * 分裂依靠Raft协议的日志进行同步，每个节点应用日志时，更新oldregion的元数据，并且创建newregion，并对其进行snapshot
     * 由于storeEngine上的store info信息是靠心跳传输到pd上的，所以内存中的分裂之后的region信息可能无法传到pd上，比如心跳之前挂机
     * 那么storeEngine重启之后可能从pd上查询得到的region信息是分裂之前的。
     * 但是由于region split是以raft日志的形式应用，所以宕机重启之后可以再次应用日志，从而获得分裂信息
     *
     * 分裂时的读写一致性
     *TODO：
     * 分裂时读取：
     * 因为客户端读取首先访问pd上的信息，并且在访问storeEngine时要带需要访问的region的epoch
     * 如果获取到pd上分裂后的信息，但是访问的storeEngine却还未应用分裂,则访问失败，客户端需要重试
     * 如果获取到pd上分裂前的信息，但是访问的storeEngine已经分裂，则访问失败，需要重试
     * 其余情况都可以正常访问，但是可能不满足线性一致性
     * 比如：
     * Split 之后原 Region 的范围缩小，其余的范围属于新 Region，而新 Region 存活的 Peer 个数已经超过了 Raft 所要求的大多数副本，故可以合理的发起选举并产生 Leader，并且正常服务读写请求。此时原 Region Leader 仍然还未应用 Split Proposal，如果因为持有租约继续服务原范围的读请求，就会破坏线性一致性。
     * 解决方案是，在region分裂的时候，停止被分裂的region提供的读取操作
     * https://segmentfault.com/a/1190000023869223
     *
     *
     * 分裂时写入：
     * 因为客户端写入也是要带epoch的，所以所有写入都要在propose和apply时进行epoch的检测
     * 因为raft日志是有序apply的，所以如果有数据的写入是在region分裂之后进行的，那么写入日志一定是在分裂日志后
     * 1. 如果写入的节点还未应用region分裂日志，那么写入日志和分裂日志都是写入被分裂的region的raft log中，
     *      那么当应用写入日志时，分裂日志已经被应用，region的epoch增加，导致写入日志应用被skip，写入失败。
     * 2. 如果写入的节点已经应用region分裂日志，那么则正常写入
     *
     *
     * 分裂耗时导致，pd发送多次分裂指令：
     * 1. 通过判断region是否正在分裂，决定是否进行分裂操作
     * 2. 如果region的分裂日志已经被应用，那么regionEpoch一定会过期，则跳过分裂
     * 3. 如果region的分裂日志已经写入还未被应用，则regionEngine一定显示正在分裂中，通过判断lastSplitIndex和lastAppliedIndex的大小
     * 4， 如果region的分裂日志还未写入，则进行分裂
     *
     * @param regionId
     */
    public void splitRegion(long regionId, long regionEpoch){
        RegionEngine regionEngine = regionEngineTable.get(regionId);
        if (regionEngine == null) {
            log.warn("can not find regionEngine : " + regionId);
            return;
        }

        regionEngine.split(regionEpoch);
    }


}
