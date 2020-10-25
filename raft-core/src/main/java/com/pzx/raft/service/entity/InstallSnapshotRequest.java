package com.pzx.raft.service.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * @author PZX
 */
@Getter
@Builder
@ToString
public class InstallSnapshotRequest {

    /**
     * 领导人的任期号
     */
    private final long leaderTerm;

    /**
     * 领导人的 Id，以便于跟随者重定向请求
     */
    private final int leaderId;

    /**
     * 快照中包含的最后日志条目的索引值
     */
    private final long lastIncludedIndex;

    /**
     * 快照中包含的最后日志条目的任期号
     */
    private final long lastIncludedTerm;

    /**
     * 分块在快照中的字节偏移量
     */
    private final long offset;

    /**
     * 从偏移量开始的快照分块的原始字节
     */
    private final byte[] data;

    /**
     * 如果这是最后一个分块则为 true
     */
    private final boolean done;


}
