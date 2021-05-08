package com.pzx.raft.kv;


import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.core.node.RaftNode;
import com.pzx.raft.core.storage.KVRaftStateMachine;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class RaftKVStore implements AsyncKVStore {

    RaftNode raftNode;

    KVRaftStateMachine raftStateMachine;

    public RaftKVStore(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.raftStateMachine = (KVRaftStateMachine) raftNode.getRaftStateMachine();
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

        return raftNode.replicateEntry(command);
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
        return raftNode.replicateEntry(command);
    }

    @Override
    public long getApproximateKeysInRange(byte[] startKey, byte[] endKey) {
        return raftStateMachine.getKvPrefixAdapter().getApproximateKeysInRange(startKey, endKey);
    }

    @Override
    public byte[] jumpOver(byte[] startKey, long distance) {
        return raftStateMachine.getKvPrefixAdapter().jumpOver(startKey, distance);
    }
}
