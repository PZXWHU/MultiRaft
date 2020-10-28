package com.pzx.raft.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum  RaftError {

    CONFIG_FILE_LOAD_FAILED("配置文件加载失败"),
    NODE_RECOVER_FAILED("节点重启失败"),
    LOAD_RAFTLOG_ERROR("加载raftLog出错！"),
    LOAD_SNAPSHOT_ERROR("加载snapshot出错！");

    private final String message;
}
