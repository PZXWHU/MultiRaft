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
public class ReqVoteResponse {

    /**
     * 当前任期号，以便于候选人去更新自己的任期号
     */
    private final long currentTerm;

    /**
     * 候选人赢得了此张选票时为真
     */
    private final long voteGranted;

    public ReqVoteResponse(long currentTerm, long voteGranted) {
        this.currentTerm = currentTerm;
        this.voteGranted = voteGranted;
    }
}
