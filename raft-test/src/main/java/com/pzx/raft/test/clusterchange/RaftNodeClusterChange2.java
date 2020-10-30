package com.pzx.raft.test.clusterchange;

import com.pzx.raft.node.RaftNode;
import com.pzx.raft.test.election.RaftNodeElection1;

public class RaftNodeClusterChange2 {

    public static void main(String[] args) {
        String configPath = RaftNodeElection1.class.getClassLoader().getResource("config4.json").getPath();
        RaftNode raftNode = new RaftNode(configPath);
        raftNode.start();
    }

}
