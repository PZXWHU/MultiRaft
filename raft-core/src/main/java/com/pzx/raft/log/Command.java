package com.pzx.raft.log;

import lombok.*;

import java.util.Objects;

/**
 * LogEntry中包含的指令，一般为set key value
 * @author PZX
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class Command {

    private final String key;

    private final Object value;

    public Command(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}
