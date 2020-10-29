package com.pzx.raft.service.entity;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterMembershipChangeRequest {

    private int nodeId;

    private String nodeAddress;

}
