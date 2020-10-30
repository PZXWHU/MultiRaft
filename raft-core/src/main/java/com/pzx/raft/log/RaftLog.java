package com.pzx.raft.log;

import com.pzx.raft.node.NodePersistMetadata;

import java.util.concurrent.locks.Lock;

/**
 * @author PZX
 */
public interface RaftLog {

    /**
     * 为logEntry赋值Index，并写入日志
     * @param logEntry
     */
    long write(LogEntry logEntry);

    /**
     * 读取指定位置的logEntry
     * @param index
     * @return
     */
    LogEntry read(long index);

    /**
     * 删除startIndex以及其之后的所有日志条目
     * @param startIndex
     */
    void removeFromStartIndex(Long startIndex);

    /**
     * 删除endIndex以及其之前的所有日志条目
     * @param endIndex
     */
    void removeToEndIndex(Long endIndex);

    /**
     * 获取最大的Index
     * @return
     */
    long getLastIndex();

    /**
     * 获取目前日志中的日志条目数目
     * @return
     */
    long getTotalSize();

    /**
     * 获取最大的Index对应的日志条目
     * @return return null if the last LogEntry is not in RaftLog but in Snapshot
     */
    default LogEntry getLast(){
        return read(getLastIndex());
    }

    /**
     * 获取日志的持久化的节点元数据
     * @return
     */
    NodePersistMetadata getNodePersistMetaData();

    /**
     * raft节点日志的元数据持久化，用于节点重启恢复
     * @param other
     * @return
     */
    void updateNodePersistMetaData(NodePersistMetadata other);


    Lock getLock();

}
