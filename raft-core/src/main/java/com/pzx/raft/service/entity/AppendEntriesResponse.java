package com.pzx.raft.service.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * @author PZX
 */
@Builder
@Getter
@ToString
public class AppendEntriesResponse {

    /** 当前的任期号，用于领导人去更新自己 */
    private final long currentTerm;

    /** 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真  */
    private final boolean success;

    public AppendEntriesResponse(long currentTerm, boolean success) {
        this.currentTerm = currentTerm;
        this.success = success;
    }



}
