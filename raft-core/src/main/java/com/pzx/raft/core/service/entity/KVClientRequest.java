package com.pzx.raft.core.service.entity;

import com.pzx.raft.core.entity.KVOperation;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KVClientRequest {

    KVOperation kvOperation;

    byte[] key;

    byte[] value;
}
