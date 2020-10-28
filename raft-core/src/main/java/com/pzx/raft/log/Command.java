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
@NoArgsConstructor
@AllArgsConstructor
public class Command {

    private String key;

    private Object value;


}
