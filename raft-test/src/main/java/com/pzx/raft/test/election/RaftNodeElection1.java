package com.pzx.raft.test.election;

import com.pzx.raft.node.RaftNode;

import java.io.IOException;

public class RaftNodeElection1 {
    public static void main(String[] args) throws IOException {
        String configPath = RaftNodeElection1.class.getClassLoader().getResource("config1.json").getPath();
        RaftNode raftNode = new RaftNode(configPath);
        raftNode.start();

    }
}
