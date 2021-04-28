package com.pzx.raft.core.entity;

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

}
