package com.pzx.raft.core.service.entity;

import lombok.*;

@Setter
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientKVResponse {

    boolean success;

    String message;

    byte[] result;
}
