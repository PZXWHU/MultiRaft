package com.pzx.raft.test.snapshot;

import com.pzx.raft.node.RaftNode;
import com.pzx.raft.test.election.RaftNodeElection1;
import com.pzx.raft.utils.ThreadPoolUtils;

import java.util.concurrent.TimeUnit;

/**
 * 节点生成快照之后
 */
public class RaftNodeSnapshot1 {

    public static void main(String[] args) {
        String configPath = RaftNodeElection1.class.getClassLoader().getResource("config1.json").getPath();
        final RaftNode raftNode = new RaftNode(configPath);
        ThreadPoolUtils.getScheduledThreadPool().schedule(()->{
            raftNode.takeSnapshot();
        }, 10, TimeUnit.SECONDS);
        raftNode.start();
    }

}
