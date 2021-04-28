package com.pzx.raft.kv;

import com.pzx.raft.core.utils.ThreadPoolUtils;
import com.pzx.raft.kv.config.PdConfig;
import com.pzx.raft.kv.config.StoreConfig;
import com.pzx.raft.kv.entity.Region;
import com.pzx.raft.kv.service.impl.RaftGroupServiceImpl;
import com.pzx.rpc.transport.RpcServer;
import com.pzx.rpc.transport.netty.server.NettyServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

@Getter
@Slf4j
public class StoreEngine {

    private long storeId;

    private RpcServer rpcServer;

    private PerKVStore perKVStore;

    private KVStore memKVStore;

    private StoreConfig storeConfig;

    private PdConfig pdConfig;

    private PdClient pdClient;

    private StoreMetaStorage storeMetaStorage;

    private ExecutorService executorService;

    private final ConcurrentMap<Long, RegionEngine> regionEngineTable  = new ConcurrentHashMap<>();

    public StoreEngine(StoreConfig storeConfig, PdConfig pdConfig) {
        this.storeConfig = storeConfig;
        this.perKVStore = new RocksKVStore(storeConfig.getRocksDBPath());
        this.memKVStore = perKVStore;
        this.storeMetaStorage = new StoreMetaStorage(perKVStore);
        this.pdClient = new PdClient(pdConfig);
        this.executorService = ThreadPoolUtils.newThreadPoolExecutor(storeConfig.getExecutorThreadNum(),
                storeConfig.getExecutorThreadNum(), 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());


        recover();
        initRpcServer();
    }

    private void initRpcServer(){
        String[] storeAddress = storeConfig.getStoreAddress().split(":");
        rpcServer = NettyServer.builder()
                .autoScanService(false)
                .serverAddress(new InetSocketAddress(storeAddress[0], Integer.valueOf(storeAddress[1])))
                .build();
        rpcServer.publishService(new RaftGroupServiceImpl(this));
    }

    private void recover(){
        if (storeMetaStorage.getRegions() != null){
            for(Region region : storeMetaStorage.getRegions()){
                executorService.submit(() -> regionEngineTable.put(region.getRegionId(), new RegionEngine(region, this)));
            }
        }
    }

    public RegionEngine createRegion(Region region){
        RegionEngine regionEngine = new RegionEngine(region, this);
        regionEngineTable.put(region.getRegionId(), regionEngine);
        log.info("create new region :" + region);
        return null;
    }



}
