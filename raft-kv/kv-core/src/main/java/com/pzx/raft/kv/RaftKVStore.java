package com.pzx.raft.kv;


import com.pzx.raft.core.RaftStateMachine;
import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.core.node.RaftNode;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class RaftKVStore implements AsyncKVStore {

    RaftNode raftNode;

    RaftStateMachine raftStateMachine;

    public RaftKVStore(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.raftStateMachine = raftNode.getRaftStateMachine();
    }

    @Override
    public CompletableFuture<byte[]> getAsync(byte[] key) {
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        completableFuture.complete(raftStateMachine.get(key));
        return completableFuture;
    }

    @Override
    public CompletableFuture<Boolean> putAsync(byte[] key, byte[] value) {
        SMCommand command = new SMCommand(key, value);
        LogEntry logEntry = LogEntry.builder().term(raftNode.getCurrentTerm()).command(command).build();
        return raftNode.replicateEntry(logEntry);
    }

    @Override
    public CompletableFuture<List<byte[]>> scanAsync(byte[] startKey, byte[] endKey) {
        CompletableFuture<List<byte[]>> completableFuture = new CompletableFuture<>();
        completableFuture.complete(raftStateMachine.scan(startKey, endKey));
        return completableFuture;
    }

    @Override
    public CompletableFuture<Boolean> deleteAsync(byte[] key) {
        SMCommand command = new SMCommand(key, null);
        LogEntry logEntry = LogEntry.builder().term(raftNode.getCurrentTerm()).command(command).build();
        return raftNode.replicateEntry(logEntry);
    }
}
