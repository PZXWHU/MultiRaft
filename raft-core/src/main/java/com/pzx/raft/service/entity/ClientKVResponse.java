package com.pzx.raft.service.entity;

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

    Object result;
}
