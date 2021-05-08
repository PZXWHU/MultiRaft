package com.pzx.raft.test.core;

import com.pzx.raft.core.RaftLogStorage;
import com.pzx.raft.core.RaftMetaStorage;
import com.pzx.raft.core.RaftStateMachine;
import com.pzx.raft.core.config.NodeConfig;
import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.core.node.RaftNode;
import com.pzx.raft.core.service.RaftKVClientService;
import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.core.service.impl.RaftConsensusServiceImpl;
import com.pzx.raft.core.service.impl.RaftKVClientServiceImpl;
import com.pzx.raft.core.storage.KVRaftLogStorage;
import com.pzx.raft.core.storage.KVRaftMetaStorage;
import com.pzx.raft.core.storage.KVRaftStateMachine;
import com.pzx.raft.kv.RocksKVStore;
import com.pzx.rpc.transport.RpcServer;
import com.pzx.rpc.transport.netty.server.NettyServer;
import lombok.Getter;

import java.io.File;
import java.net.InetSocketAddress;

@Getter
public class Server {

    private RpcServer rpcServer;

    private RaftNode raftNode;

    public Server(RaftConfig raftConfig, String raftHome, long serverId, String ip, int port){

        long groupId = 1;
        RocksKVStore rocksKVStore = new RocksKVStore(raftHome + File.separator + "data");
        RaftStateMachine raftStateMachine = new KVRaftStateMachine(rocksKVStore);

        RaftMetaStorage raftMetaStorage = new KVRaftMetaStorage(groupId, rocksKVStore);
        RaftLogStorage raftLogStorage = new KVRaftLogStorage(groupId, rocksKVStore);

        NodeConfig nodeConfig = NodeConfig.builder()
                .serverId(serverId)
                .groupId(groupId)
                .nodeHome(raftHome)
                .raftConfig(raftConfig)
                .raftLogStorage(raftLogStorage)
                .raftMetaStorage(raftMetaStorage)
                .raftStateMachine(raftStateMachine)
                .build();
        raftNode = new RaftNode(nodeConfig);

        rpcServer = NettyServer.builder()
                .serverAddress(new InetSocketAddress(ip, port))
                .autoScanService(false)
                .build();

        rpcServer.publishService(new RaftConsensusServiceImpl(raftNode), RaftConsensusService.class.getCanonicalName());
        rpcServer.publishService(new RaftKVClientServiceImpl(raftNode), RaftKVClientService.class.getCanonicalName());

    }

    public void start(){
        rpcServer.start();
        raftNode.start();
    }
}
