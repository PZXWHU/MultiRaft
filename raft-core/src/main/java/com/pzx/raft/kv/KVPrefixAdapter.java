package com.pzx.raft.kv;

import com.pzx.raft.core.utils.ByteUtils;
import lombok.Getter;

import java.util.Arrays;
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
        key = mergePrefix(prefix, key);
        return kvStore.get(key);
    }

    @Override
    public boolean put(byte[] key, byte[] value) {
        key = mergePrefix(prefix, key);
        return kvStore.put(key, value);
    }

    @Override
    public boolean delete(byte[] key) {
        key = mergePrefix(prefix, key);
        return kvStore.delete(key);
    }

    @Override
    public List<byte[]> scan(byte[] startKey, byte[] endKey) {
        startKey = mergePrefix(prefix, startKey);
        endKey = mergePrefix(prefix, endKey);
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

    @Override
    public long getApproximateKeysInRange(byte[] startKey, byte[] endKey) {
        return kvStore.getApproximateKeysInRange(mergePrefix(prefix, startKey), mergePrefix(prefix, endKey));
    }

    @Override
    public byte[] jumpOver(byte[] startKey, long distance) {
        return removePrefix(prefix, kvStore.jumpOver(mergePrefix(prefix, startKey), distance));
    }

    public static byte[] removePrefix(String prefix, byte[] bytes){
        byte[] prefixBytes = ByteUtils.stringToBytes(prefix);
        return Arrays.copyOfRange(bytes, prefix.length(), bytes.length);
    }

    public static byte[] mergePrefix(String prefix, byte[] bytes){
        //当bytes == null，说明需要返回拥有prefix的最大字节数组，这是无法取到的，只能返回更大的prefix
        if (bytes == null){
            char[] chars = prefix.toCharArray();
            chars[chars.length - 1]++;
            prefix = new String(chars);
        }
        return ByteUtils.merge(ByteUtils.stringToBytes(prefix), bytes);
    }

}
