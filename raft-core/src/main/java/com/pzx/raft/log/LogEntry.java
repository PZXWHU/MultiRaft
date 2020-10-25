package com.pzx.raft.log;

import lombok.*;

/**
 * @author PZX
 */
@Setter
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class LogEntry {

    private long index;

    private long term;

    private Command command;

    public LogEntry(long index, long term, Command command) {
        this.index = index;
        this.term = term;
        this.command = command;
    }
}
