package com.pzx.raft.core.service.entity;

import lombok.*;

/**
 * @author PZX
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReqVoteResponse {

    /**
     * 当前任期号，以便于候选人去更新自己的任期号
     */
    private long currentTerm;

    /**
     * 候选人赢得了此张选票时为真
     */
    private boolean voteGranted;

}
