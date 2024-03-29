package com.pzx.raft.test.core.snapshot;

import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.test.core.Server;
import com.pzx.raft.core.utils.ThreadPoolUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 节点生成快照之后
 */
public class RaftNodeSnapshot1 {

    public static void main(String[] args) {

        Map<Long, String> raftGroupAddress = new HashMap<Long, String>(){
            {
                put(1L, "127.0.0.1:8001");
                put(2L, "127.0.0.1:8002");
                put(3L, "127.0.0.1:8003");
            }
        };;
        String raftHome  = "C:\\Users\\PZX\\Desktop\\raft\\raftHome1";
        long serverId = 1;
        String ip = "127.0.0.1";
        int port = 8001;

        RaftConfig raftConfig = new RaftConfig();
        raftConfig.setRaftGroupAddress(raftGroupAddress);
        raftConfig.setSnapshotMinLogSize(70);

        Server server = new Server(raftConfig, raftHome, serverId, ip, port);

        ThreadPoolUtils.getScheduledThreadPool().schedule(()->{
            server.getRaftNode().takeSnapshot();
        }, 10, TimeUnit.SECONDS);

        server.start();
    }

}
