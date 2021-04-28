package com.pzx.raft.core.test.clusterchange;

import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.core.test.Server;

public class RaftNodeClusterChange2 {

    public static void main(String[] args) {
        String raftGroupAddress = "1-127.0.0.1:8001,2-127.0.0.1:8002,3-127.0.0.1:8003,4-127.0.0.1:8004";
        String raftHome  = "C:\\Users\\PZX\\Desktop\\raft\\raftHome4";
        long serverId = 4;
        String ip = "127.0.0.1";
        int port = 8004;

        RaftConfig raftConfig = new RaftConfig();
        raftConfig.setRaftGroupAddress(raftGroupAddress);
        //raftConfig.setSnapshotMinLogSize(70);

        Server server = new Server(raftConfig, raftHome, serverId, ip, port);
        server.getRpcServer().start();
    }

}
