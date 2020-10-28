package com.pzx.raft.service.entity;

import lombok.*;
import sun.rmi.runtime.Log;

/**
 * @author PZX
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReqVoteRequest {

    /**
     *候选人的任期号
     */
    private long candidateTerm;

    /**
     * 请求选票的候选人的 Id
     */
    private int candidateId;

    /**
     * 候选人的最后日志条目的索引值
     */
    private long lastLogIndex;

    /**
     * 候选人最后日志条目的任期号
     */
    private long lastLogTerm;
}
