package com.pzx.raft.core.test.snapshot;

import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.core.test.Server;
import com.pzx.raft.core.utils.ThreadPoolUtils;

import java.util.concurrent.TimeUnit;

/**
 * 节点生成快照之后
 */
public class RaftNodeSnapshot1 {

    public static void main(String[] args) {

        String raftGroupAddress = "1-127.0.0.1:8001,2-127.0.0.1:8002,3-127.0.0.1:8003";
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
