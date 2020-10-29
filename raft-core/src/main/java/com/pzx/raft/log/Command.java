package com.pzx.raft.log;

import lombok.*;

import java.util.Objects;

/**
 * LogEntry中包含的指令，一般为set key value
 * @author PZX
 */
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Command {

    public enum CommandType{
        DATA,CONFIGURATION;
    }

    private String key;

    private Object value;

    private CommandType commandType;//默认为DATA

    public Command(String key, Object value){
        this.key = key;
        this.value = value;
        this.commandType = CommandType.DATA;
    }


}
