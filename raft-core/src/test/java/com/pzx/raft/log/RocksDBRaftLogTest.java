package com.pzx.raft.log;

import com.pzx.raft.node.NodePersistMetadata;
import org.junit.Test;

public class RocksDBRaftLogTest {

    RaftLog raftLog = new RocksDBRaftLog("D:\\test\\log");

    @Test
    public void write() {

        for(int i = 1; i < 100; i++){
            LogEntry logEntry = new LogEntry(0, 1, new Command(i + "", i));
            raftLog.write(logEntry);
        }
    }

    @Test
    public void read() {
        for(int i = 1; i < 100; i++){
            System.out.println(raftLog.read(i));
        }
    }

    @Test
    public void removeFromStartIndex() {
        raftLog.removeFromStartIndex(50l);
        for(int i = 1; i < 100; i++){
            System.out.println(raftLog.read(i));
        }
    }

    @Test
    public void removeToEndIndex() {
        raftLog.removeToEndIndex(25l);
        for(int i = 1; i < 100; i++){
            System.out.println(raftLog.read(i));
        }
    }

    @Test
    public void getLastIndex() {
        System.out.println(raftLog.getLastIndex());
    }

    @Test
    public void getTotalSize() {
        System.out.println(raftLog.getTotalSize());
    }

    @Test
    public void getNodePersistMetaData() {
        System.out.println(raftLog.getNodePersistMetaData());
    }

    @Test
    public void updateNodePersistMetaData() {
        raftLog.updateNodePersistMetaData(NodePersistMetadata.builder().commitIndex(22l).currentTerm(1l).votedFor(1).build());
    }
}