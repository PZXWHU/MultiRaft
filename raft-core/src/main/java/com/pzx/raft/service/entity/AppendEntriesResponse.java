package com.pzx.raft.service.entity;

import lombok.*;

/**
 * @author PZX
 */
@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AppendEntriesResponse {

    /** 当前的任期号，用于领导人去更新自己 */
    private long currentTerm;

    /** 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真  */
    private boolean success;

}
