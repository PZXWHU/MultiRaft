package com.pzx.raft.test.core.election;

import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.test.core.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RaftNodeElection2 {
    public static void main(String[] args) throws IOException {

        Map<Long, String> raftGroupAddress = new HashMap<Long, String>(){
            {
                put(1L, "127.0.0.1:8001");
                put(2L, "127.0.0.1:8002");
                put(3L, "127.0.0.1:8003");
            }
        };
        String raftHome  = "C:\\Users\\PZX\\Desktop\\raft\\raftHome2";
        long serverId = 2;
        String ip = "127.0.0.1";
        int port = 8002;

        RaftConfig raftConfig = new RaftConfig();
        raftConfig.setRaftGroupAddress(raftGroupAddress);
        //raftConfig.setSnapshotMinLogSize(70);

        Server server = new Server(raftConfig, raftHome, serverId, ip, port);
        server.start();

    }
}
