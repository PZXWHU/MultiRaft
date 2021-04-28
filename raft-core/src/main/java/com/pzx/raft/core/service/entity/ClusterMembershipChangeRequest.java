package com.pzx.raft.core.service.entity;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterMembershipChangeRequest {

    private long groupId;

    private long serverId;

    private String serverAddress;

}
