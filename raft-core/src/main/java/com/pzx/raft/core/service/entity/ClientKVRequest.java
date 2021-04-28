package com.pzx.raft.core.service.entity;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientKVRequest {
    public static int PUT = 0;
    public static int GET = 1;

    int type;

    byte[] key;

    byte[] value;
}
