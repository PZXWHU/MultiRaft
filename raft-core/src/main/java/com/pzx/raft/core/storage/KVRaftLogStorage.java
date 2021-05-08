package com.pzx.raft.core.storage;

import com.pzx.raft.core.RaftLogStorage;
import com.pzx.raft.kv.KVPrefixAdapter;
import com.pzx.raft.kv.PerKVStore;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.core.utils.ProtobufSerializerUtils;
import com.pzx.raft.core.entity.LogEntry;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.locks.Lock;

@Slf4j
public class KVRaftLogStorage implements RaftLogStorage {

    public final static String LAST_INDEX = "lastIndex";

    public final static String TOTAL_SIZE = "totalSize";

    private final static String LOG_PREFIX = "log_";

    private KVPrefixAdapter kvPrefixAdapter;

    private long groupId;

    private String prefix;

    public KVRaftLogStorage(long groupId, PerKVStore kvStore) {
        this.groupId = groupId;
        this.prefix = LOG_PREFIX + groupId + "_";
        this.kvPrefixAdapter = new KVPrefixAdapter(kvStore, prefix);
    }

    @Override
    public long write(LogEntry logEntry) {
        boolean success = false;
        long lastIndex = -1;
        kvPrefixAdapter.getWriteLock().lock();
        try{
            logEntry.setIndex(getLastIndex() + 1);
            success = kvPrefixAdapter.put(ByteUtils.longToBytes(logEntry.getIndex()), ProtobufSerializerUtils.serialize(logEntry));
        }finally {
            if (success){
                logEntry.onWrite(this);//触发logEntry写时间
                kvPrefixAdapter.put(ByteUtils.stringToBytes(LAST_INDEX), ByteUtils.longToBytes(logEntry.getIndex()));
                kvPrefixAdapter.put(ByteUtils.stringToBytes(TOTAL_SIZE), ByteUtils.longToBytes(getTotalSize() + 1));
            }
            lastIndex = getLastIndex();
            kvPrefixAdapter.getWriteLock().unlock();
        }

        return lastIndex;
    }

    @Override
    public LogEntry read(long index) {
        LogEntry logEntry = null;
        byte[] bytes = kvPrefixAdapter.get(ByteUtils.longToBytes(index));
        if (bytes != null)
            logEntry =  (LogEntry) ProtobufSerializerUtils.deserialize(bytes, LogEntry.class);
        return logEntry;
    }

    @Override
    public void removeFromStartIndex(Long startIndex) {
        int count = 0;
        kvPrefixAdapter.getWriteLock().lock();
        try {
            for (long i = getLastIndex(); i >= startIndex; i--) {
                kvPrefixAdapter.delete(ByteUtils.longToBytes(i));
                count++;
            }
        }finally {
            kvPrefixAdapter.put(ByteUtils.stringToBytes(LAST_INDEX), ByteUtils.longToBytes(getLastIndex() - count));
            kvPrefixAdapter.put(ByteUtils.stringToBytes(TOTAL_SIZE), ByteUtils.longToBytes(getTotalSize() - count));
            kvPrefixAdapter.getWriteLock().unlock();
        }
    }

    @Override
    public void removeToEndIndex(Long endIndex) {
        int count = 0;
        kvPrefixAdapter.getWriteLock().lock();
        long firstIndex = getLastIndex() - getTotalSize() + 1;
        try {
            for (long i = firstIndex; i <= endIndex; i++) {
                kvPrefixAdapter.delete(ByteUtils.longToBytes(i));
                count++;
            }
        }finally {
            kvPrefixAdapter.put(ByteUtils.stringToBytes(TOTAL_SIZE), ByteUtils.longToBytes(getTotalSize() - count));
            kvPrefixAdapter.getWriteLock().unlock();
        }
    }

    @Override
    public long getLastIndex() {
        long lastIndex = 0;

        byte[] bytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(LAST_INDEX));
        if (bytes != null)
            lastIndex =  ByteUtils.bytesToLong(bytes);

        return lastIndex;
    }

    @Override
    public long getTotalSize() {
        long totalSize = 0;

        byte[] bytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(TOTAL_SIZE));
        if (bytes != null)
            totalSize =  ByteUtils.bytesToLong(bytes);

        return totalSize;
    }

    @Override
    public Lock getWriteLock() {
        return kvPrefixAdapter.getWriteLock();
    }

    @Override
    public Lock getReadLock() {
        return kvPrefixAdapter.getReadLock();
    }

}
