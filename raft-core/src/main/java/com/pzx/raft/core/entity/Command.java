package com.pzx.raft.core.entity;

import lombok.*;

import java.util.Objects;

/**
 * LogEntry中包含的指令，一般为set key value
 * @author PZX
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public abstract class Command<T, U> {
    private T key;

    private U value;

}
