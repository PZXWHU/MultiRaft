package com.pzx.raft.core.storage;

import com.pzx.raft.core.RaftStateMachine;
import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.kv.KVStore;
import com.pzx.raft.kv.KVPrefixAdapter;


import java.io.File;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class KVRaftStateMachine implements RaftStateMachine {

    protected static final String STATEMACHINE_PREFIX = "sm_";

    protected KVPrefixAdapter kvPrefixAdapter;

    protected final static String SNAPSHOT_RAFT_STATEMACHINE_FILENAME = "raftStateMachine";

    public KVRaftStateMachine(KVStore kvStore) {
        this.kvPrefixAdapter = new KVPrefixAdapter(kvStore, STATEMACHINE_PREFIX);
    }

    @Override
    public void apply(SMCommand smCommand) {
        kvPrefixAdapter.put(smCommand.getKey(), smCommand.getValue());
    }

    @Override
    public void writeSnapshot(String snapshotDirPath) throws Exception {
        snapshotDirPath += File.separator + SNAPSHOT_RAFT_STATEMACHINE_FILENAME;
        kvPrefixAdapter.writeSnapshot(snapshotDirPath);
    }

    @Override
    public void readSnapshot(String snapshotDirPath) throws Exception {
        snapshotDirPath += File.separator + SNAPSHOT_RAFT_STATEMACHINE_FILENAME;
        kvPrefixAdapter.readSnapshot(snapshotDirPath);
    }

    @Override
    public byte[] get(byte[] key) { return kvPrefixAdapter.get(key); }

    @Override
    public boolean put(byte[] key, byte[] value) {
        return kvPrefixAdapter.put(key, value);
    }

    @Override
    public boolean delete(byte[] key) {
        return kvPrefixAdapter.delete(key);
    }

    @Override
    public List<byte[]> scan(byte[] startKey, byte[] endKey) {
        return kvPrefixAdapter.scan(startKey, endKey);
    }

    @Override
    public Lock getWriteLock() {
        return kvPrefixAdapter.getWriteLock();
    }

    @Override
    public Lock getReadLock() {
        return kvPrefixAdapter.getReadLock();
    }

    public KVPrefixAdapter getKvPrefixAdapter() {
        return kvPrefixAdapter;
    }
}
