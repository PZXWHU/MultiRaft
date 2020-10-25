package com.pzx.raft.statemachine;

import com.pzx.raft.KVDataBase;
import com.pzx.raft.log.LogEntry;
import com.pzx.raft.snapshot.Snapshot;

/**
 * @author PZX
 */
public interface StateMachine extends Snapshot, KVDataBase {

    /**
     * 将数据应用到状态机.
     *
     * @param logEntry 日志中的数据.
     */
    void apply(LogEntry logEntry);

    StateMachine copy();

}
