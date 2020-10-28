package com.pzx.raft.service.entity;

import lombok.*;

/**
 * @author PZX
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class InstallSnapshotRequest {

    /**
     * 领导人的任期号
     */
    private long leaderTerm;

    /**
     * 领导人的 Id，以便于跟随者重定向请求
     */
    private int leaderId;

    /**
     * 快照中包含的最后日志条目的索引值
     */
    private long lastIncludedIndex;

    /**
     * 快照中包含的最后日志条目的任期号
     */
    private long lastIncludedTerm;

    /**
     * 分块在快照中的字节偏移量
     */
    private long offset;

    /**
     * 快照文件的名称
     */
    private String snapshotFileName;

    /**
     * 从偏移量开始的快照分块的原始字节
     */
    private byte[] data;

    /**
     * 如果这是最后一个分块则为 true
     */
    private boolean done;


}
