package com.pzx.raft.core.entity;

import com.pzx.raft.core.RaftLogStorage;
import lombok.*;

/**
 * @author PZX
 */
@Setter
@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    private long index;

    private long term;

    private Command command;

    public void onWrite(RaftLogStorage raftLogStorage){

    }


}
