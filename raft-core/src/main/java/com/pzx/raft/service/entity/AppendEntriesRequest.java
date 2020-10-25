package com.pzx.raft.service.entity;

import com.pzx.raft.log.LogEntry;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 *
 * @author PZX
 */
@Getter
@Builder
@ToString
public class AppendEntriesRequest {

    /**
     * 领导者的任期
     */
    private final long leaderTerm;

    /**
     * 领导者ID,因此跟随者可以对客户端进行重定向
     */
    private final int leaderId;

    /**
     * 紧邻新日志条目之前的那个日志条目的索引
     */
    private final long prevLogIndex;

    /**
     * 紧邻新日志条目之前的那个日志条目的任期
     */
    private final long prevLogTerm;

    /**
     * 需要被保存的日志条目（被当做心跳使用是 则日志条目内容为空；为了提高效率可能一次性发送多个）
     */
    private final LogEntry[] entries;

    /**
     * 领导者的已知已提交的最高的日志条目的索引
     */
    private final long leaderCommit;

}
