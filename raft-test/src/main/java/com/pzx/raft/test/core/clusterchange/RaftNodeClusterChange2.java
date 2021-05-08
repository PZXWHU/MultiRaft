package com.pzx.raft.test.core.clusterchange;

import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.test.core.Server;

import java.util.HashMap;
import java.util.Map;

public class RaftNodeClusterChange2 {

    public static void main(String[] args) {
        Map<Long, String> raftGroupAddress = new HashMap<Long, String>(){
            {
                put(1L, "127.0.0.1:8001");
                put(2L, "127.0.0.1:8002");
                put(3L, "127.0.0.1:8003");
                put(4L, "127.0.0.1:8004");
            }
        };
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
