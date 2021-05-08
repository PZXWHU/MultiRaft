package com.pzx.raft.kv.entity;

import com.pzx.raft.core.entity.KVCommand;
import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.core.entity.UserDefinedCommand;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EpochSMCommand extends KVCommand<byte[], byte[]> implements UserDefinedCommand {

    private long regionEpoch;

}
