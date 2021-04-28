package com.pzx.raft.core;

import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.kv.KVStore;

/**
 * @author PZX
 */
public interface RaftStateMachine extends KVStore {

    /**
     * 将数据应用到状态机.
     *
     * @param logEntry 日志中的数据.
     */
    void apply(LogEntry logEntry);


}
