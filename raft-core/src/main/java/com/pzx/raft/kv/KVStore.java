package com.pzx.raft.kv;

import com.pzx.raft.core.Snapshot;

import java.util.List;
import java.util.concurrent.locks.Lock;

public interface KVStore extends Snapshot {

    byte[] get(byte[] key);

    boolean put(byte[] key, byte[] value);

    boolean delete(byte[] key);

    List<byte[]> scan(byte[] startKey, byte[] endKey);

    default Lock getWriteLock(){
        throw new UnsupportedOperationException();
    }

    default Lock getReadLock(){
        throw new UnsupportedOperationException();
    }

    default void writeSnapshot(String snapshotDirPath) throws Exception{
        throw new UnsupportedOperationException();
    }

    default void readSnapshot(String snapshotDirPath) throws Exception{
        throw new UnsupportedOperationException();
    }

    default long getApproximateKeysInRange(final byte[] startKey, final byte[] endKey){
        throw new UnsupportedOperationException();
    }

    default byte[] jumpOver(final byte[] startKey, final long distance){
        throw new UnsupportedOperationException();
    }
}
