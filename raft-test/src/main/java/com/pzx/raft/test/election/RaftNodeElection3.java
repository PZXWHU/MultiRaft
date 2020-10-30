package com.pzx.raft.test.election;

import com.pzx.raft.node.RaftNode;

import java.io.IOException;

public class RaftNodeElection3 {
    public static void main(String[] args) throws IOException {
        String configPath = RaftNodeElection3.class.getClassLoader().getResource("config3.json").getPath();
        RaftNode raftNode = new RaftNode(configPath);
        raftNode.start();

    }
}
