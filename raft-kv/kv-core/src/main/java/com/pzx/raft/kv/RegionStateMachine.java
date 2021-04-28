package com.pzx.raft.kv;

import com.pzx.raft.core.storage.KVRaftStateMachine;
import com.pzx.raft.kv.entity.Region;

import java.io.File;
import java.io.IOException;

public class RegionStateMachine extends KVRaftStateMachine {

    private Region region;

    public RegionStateMachine(Region region, KVStore kvStore) {
        super(kvStore);
        this.region = region;
    }

    @Override
    public void writeSnapshot(String snapshotDirPath) throws IOException {
        snapshotDirPath += File.separator + SNAPSHOT_RAFT_STATEMACHINE_FILENAME;
        byte[] startKey = KVPrefixAdapter.mergeWithPrefix(kvPrefixAdapter.getPrefix(), region.getStartKey());
        byte[] endKey = KVPrefixAdapter.mergeWithPrefix(kvPrefixAdapter.getPrefix(), region.getEndKey());
        if (kvPrefixAdapter.getKvStore() instanceof RocksKVStore)
            ((RocksKVStore) kvPrefixAdapter.getKvStore()).writeSstSnapshot(snapshotDirPath, startKey, endKey);
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public void readSnapshot(String snapshotDirPath)  {
        snapshotDirPath += File.separator + SNAPSHOT_RAFT_STATEMACHINE_FILENAME;
        if (kvPrefixAdapter.getKvStore() instanceof RocksKVStore)
            ((RocksKVStore) kvPrefixAdapter.getKvStore()).readSstSnapshot(snapshotDirPath);
        else
            throw new UnsupportedOperationException();
    }



}
