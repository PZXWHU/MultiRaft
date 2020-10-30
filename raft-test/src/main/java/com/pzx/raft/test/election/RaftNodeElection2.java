package com.pzx.raft.test.election;

import com.pzx.raft.node.RaftNode;

import java.io.IOException;

public class RaftNodeElection2 {
    public static void main(String[] args) throws IOException {
        String configPath = RaftNodeElection2.class.getClassLoader().getResource("config2.json").getPath();
        RaftNode raftNode = new RaftNode(configPath);
        raftNode.start();

    }
}
