package com.pzx.raft.service.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ClusterMembershipChangeResponse {

    private boolean success;

    private String message;

}
