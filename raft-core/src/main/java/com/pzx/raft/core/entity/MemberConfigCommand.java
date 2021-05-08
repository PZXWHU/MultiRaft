package com.pzx.raft.core.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberConfigCommand extends KVCommand<Long, String> {

    public enum Type{
        ADD,REMOVE
    }

    Type type;

    public MemberConfigCommand() {
    }

    public MemberConfigCommand(long key, String value, Type type) {
        super(key, value);
        this.type = type;
    }


}
