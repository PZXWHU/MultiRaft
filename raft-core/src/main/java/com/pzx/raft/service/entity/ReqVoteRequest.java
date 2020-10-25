package com.pzx.raft.service.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import sun.rmi.runtime.Log;

/**
 * @author PZX
 */
@Getter
@Builder
@ToString
public class ReqVoteRequest {

    /**
     *候选人的任期号
     */
    private final long candidateTerm;

    /**
     * 请求选票的候选人的 Id
     */
    private final int candidateId;

    /**
     * 候选人的最后日志条目的索引值
     */
    private final long lastLogIndex;

    /**
     * 候选人最后日志条目的任期号
     */
    private final long lastLogTerm;
}
