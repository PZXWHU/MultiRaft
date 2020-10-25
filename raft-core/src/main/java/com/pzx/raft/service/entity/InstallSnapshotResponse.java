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
public class InstallSnapshotResponse {

    private final long currentTerm;

    public InstallSnapshotResponse(long currentTerm) {
        this.currentTerm = currentTerm;
    }
}
