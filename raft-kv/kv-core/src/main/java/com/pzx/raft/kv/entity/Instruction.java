package com.pzx.raft.kv.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Instruction {

    public enum InstructionType{
        EMPTY,SPLIT
    }

    private InstructionType type;

    private Object data;

}
