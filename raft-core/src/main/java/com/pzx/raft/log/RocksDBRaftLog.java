package com.pzx.raft.log;

import com.pzx.raft.exception.RaftError;
import com.pzx.raft.exception.RaftException;
import com.pzx.raft.node.NodePersistMetadata;
import com.pzx.raft.utils.ByteUtils;
import com.pzx.raft.utils.ProtobufSerializerUtils;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RocksDBRaftLog implements RaftLog {

    private static final Logger logger = LoggerFactory.getLogger(RocksDBRaftLog.class);

    private RocksDB logDb;
    private Lock lock = new ReentrantLock();

    public final static byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();
    public final static byte[] TOTAL_SIZE_KEY = "TOTAL_SIZE_KEY".getBytes();

    static {
        RocksDB.loadLibrary();
    }

    public RocksDBRaftLog(String logDirPath){
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            logDb = RocksDB.open(options, logDirPath);
        }catch (RocksDBException e){
            throw new RaftException(RaftError.LOAD_RAFTLOG_ERROR, e.getMessage());
        }
    }

    /**
     * logEntry 的 index 就是 key. 严格保证递增.
     * @param logEntry
     */
    @Override
    public long write(LogEntry logEntry) {
        boolean success = false;
        long lastIndex;
        lock.lock();
        try {
            logEntry.setIndex(getLastIndex() + 1);
            logDb.put(ByteUtils.longToBytes(logEntry.getIndex()), ProtobufSerializerUtils.serialize(logEntry));
            success = true;
        }catch (RocksDBException e){
            logger.error("write logEntry failed : " + e);
        }finally {
            if (success){
                updateLastIndex(logEntry.getIndex());
                updateTotalSize(getTotalSize() + 1);
            }
            lastIndex = getLastIndex();
            lock.unlock();
        }
        return lastIndex;
    }

    @Override
    public LogEntry read(long index) {
        try {
            byte[] bytes = logDb.get(ByteUtils.longToBytes(index));
            if (bytes != null)
                return (LogEntry) ProtobufSerializerUtils.deserialize(bytes, LogEntry.class);
        }catch (RocksDBException e){
            logger.error("read logEntry failed : " + e);
        }
        return null;
    }

    @Override
    public void removeFromStartIndex(Long startIndex) {
        int count = 0;
        lock.lock();
        try {
            for (long i = getLastIndex(); i >= startIndex; i--) {
                logDb.delete(ByteUtils.longToBytes(i));
                count++;
            }
        }catch (RocksDBException e){
            logger.error("remove from startIndex failed! The logEntry whose index is after {} have been deleted", (getLastIndex() - count));
        }finally {
            updateLastIndex(getLastIndex() - count);
            updateTotalSize(getTotalSize() - count);
            lock.unlock();
        }

    }

    @Override
    public void removeToEndIndex(Long endIndex) {
        int count = 0;
        lock.lock();
        long firstIndex = getLastIndex() - getTotalSize() + 1;
        try {
            for (long i = firstIndex; i <= endIndex; i++) {
                logDb.delete(ByteUtils.longToBytes(i));
                count++;
            }
        }catch (RocksDBException e){
            logger.error("remove to endIndex failed! The logEntry whose index is before {} have been deleted", (firstIndex + count));
        }finally {
            updateTotalSize(getTotalSize() - count);
            lock.unlock();
        }
    }


    @Override
    public long getLastIndex() {
        try {
            byte[] bytes = logDb.get(LAST_INDEX_KEY);
            if (bytes != null)
                return ByteUtils.bytesToLong(bytes);
        }catch (RocksDBException e){
            logger.error("get firstIndex failed : " + e);
        }
        return 0l;
    }

    @Override
    public long getTotalSize() {
        try {
            byte[] bytes = logDb.get(TOTAL_SIZE_KEY);
            if (bytes != null)
                return ByteUtils.bytesToLong(bytes);
        }catch (RocksDBException e){
            logger.error("get totalSize failed : " + e);
        }
        return 0l;
    }

    @Override
    public NodePersistMetadata getNodePersistMetaData() {
        int votedFor = 0;
        long currentTerm = 0l;
        long commitIndex = 0l;
        NodePersistMetadata.LOCK.lock();
        try {
            if (logDb.get(NodePersistMetadata.VOTED_FOR_KEY) != null)
                votedFor = ByteUtils.bytesToInteger(logDb.get(NodePersistMetadata.VOTED_FOR_KEY));
            if (logDb.get(NodePersistMetadata.CURRENT_TERM_KEY) != null)
                currentTerm = ByteUtils.bytesToLong(logDb.get(NodePersistMetadata.CURRENT_TERM_KEY));
            if (logDb.get(NodePersistMetadata.COMMIT_INDEX_KEY) != null)
                commitIndex = ByteUtils.bytesToLong(logDb.get(NodePersistMetadata.COMMIT_INDEX_KEY));
        }catch (RocksDBException e){
            logger.error("get log metaData failed : " + e);
        }finally {
            NodePersistMetadata.LOCK.unlock();
        }
        return NodePersistMetadata.builder()
                .votedFor(votedFor)
                .commitIndex(commitIndex)
                .currentTerm(currentTerm)
                .build();
    }

    @Override
    public void updateNodePersistMetaData(NodePersistMetadata other) {
        NodePersistMetadata.LOCK.lock();
        try {
            if (other.getVotedFor() > 0)
                logDb.put(NodePersistMetadata.VOTED_FOR_KEY, ByteUtils.integerToBytes(other.getVotedFor()));
            if(other.getCommitIndex() > 0){
                logDb.put(NodePersistMetadata.COMMIT_INDEX_KEY, ByteUtils.longToBytes(other.getCommitIndex()));
            }
            if (other.getCurrentTerm() > 0){
                logDb.put(NodePersistMetadata.CURRENT_TERM_KEY, ByteUtils.longToBytes(other.getCurrentTerm()));
            }
        }catch (RocksDBException e){
            logger.error("update log metaData failed : " + e);
        }finally {
            NodePersistMetadata.LOCK.unlock();
        }
    }

    @Override
    public Lock getLock() {
        return this.lock;
    }

    private void updateLastIndex(long index){
        try {
            // overWrite
            logDb.put(LAST_INDEX_KEY, ByteUtils.longToBytes(index));
        } catch (RocksDBException e) {
            logger.error("update lastIndex failed : " + e);
        }
    }

    private void updateTotalSize(long size){
        try {
            // overWrite
            logDb.put(TOTAL_SIZE_KEY, ByteUtils.longToBytes(size));
        } catch (RocksDBException e) {
            logger.error("update totalSize failed : " + e);
        }
    }

}
