package com.pzx.raft.kv;

import com.pzx.raft.kv.config.PdServerConfig;
import com.pzx.raft.kv.meta.KVMetaStorage;
import com.pzx.raft.kv.meta.MetaStorage;
import com.pzx.raft.kv.service.impl.PlaceDriverServiceImpl;
import com.pzx.rpc.transport.RpcServer;
import com.pzx.rpc.transport.netty.server.NettyServer;
import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
public class PlaceDriverServer {

    private MetaStorage metaStorage;

    private PerKVStore rawKVStore;

    private Instructor instructor;

    private PdServerConfig pdServerConfig;

    private RpcServer rpcServer;

    private String serverAddress;

    private RegionRouteTable regionRouteTable;

    public PlaceDriverServer(PdServerConfig pdServerConfig) {
        this.pdServerConfig = pdServerConfig;
        this.serverAddress = pdServerConfig.getServerAddress();
        this.rawKVStore = new RocksKVStore(pdServerConfig.getRocksDBPath());
        this.metaStorage = new KVMetaStorage(rawKVStore);
        this.instructor = new DefaultInstructor(this);
        this.regionRouteTable = new RegionRouteTable(metaStorage);
        initRpcServer();
    }

    private void initRpcServer(){
        String[] pdServerAddress = serverAddress.split(":");
        this.rpcServer = NettyServer.builder()
                .serverAddress(new InetSocketAddress(pdServerAddress[0], Integer.valueOf(pdServerAddress[1])))
                .autoScanService(false)
                .build();
        rpcServer.publishService(new PlaceDriverServiceImpl(this));
    }

    public void start(){
        rpcServer.start();
    }
}
