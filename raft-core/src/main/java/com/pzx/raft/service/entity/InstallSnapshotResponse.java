package com.pzx.raft.service.entity;

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
public class InstallSnapshotResponse {

    private boolean success;

    private long currentTerm;

}
