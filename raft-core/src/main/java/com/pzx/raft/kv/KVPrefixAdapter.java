package com.pzx.raft.kv;

import com.pzx.raft.core.utils.ByteUtils;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * 带有前缀的KVStore
 */
@Getter
public class KVPrefixAdapter implements KVStore {

    private KVStore kvStore;

    private String prefix;

    public KVPrefixAdapter(KVStore kvStore, String prefix) {
        this.prefix = prefix;
        this.kvStore = kvStore;
    }

    @Override
    public byte[] get(byte[] key) {
        key = mergeWithPrefix(prefix, key);
        return kvStore.get(key);
    }

    @Override
    public boolean put(byte[] key, byte[] value) {
        key = mergeWithPrefix(prefix, key);
        return kvStore.put(key, value);
    }

    @Override
    public boolean delete(byte[] key) {
        key = mergeWithPrefix(prefix, key);
        return kvStore.delete(key);
    }

    @Override
    public List<byte[]> scan(byte[] startKey, byte[] endKey) {
        startKey = mergeWithPrefix(prefix, startKey);
        endKey = mergeWithPrefix(prefix, endKey);
        return kvStore.scan(startKey, endKey);
    }

    @Override
    public Lock getWriteLock() {
        return kvStore.getWriteLock();
    }

    @Override
    public Lock getReadLock() {
        return kvStore.getReadLock();
    }

    @Override
    public void writeSnapshot(String snapshotDirPath) throws Exception {
        kvStore.writeSnapshot(snapshotDirPath);
    }

    @Override
    public void readSnapshot(String snapshotDirPath) throws Exception {
        kvStore.readSnapshot(snapshotDirPath);
    }

    public static byte[] mergeWithPrefix(String prefix, byte[] bytes){
        return ByteUtils.merge(ByteUtils.stringToBytes(prefix), bytes);
    }

}
