package com.pzx.raft.core;

import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.kv.KVStore;

/**
 * @author PZX
 */
public interface RaftStateMachine extends KVStore {

    /**
     * 将数据应用到状态机.
     *
     * @param smCommand 日志中的指令.
     */
    void apply(SMCommand smCommand);


}
