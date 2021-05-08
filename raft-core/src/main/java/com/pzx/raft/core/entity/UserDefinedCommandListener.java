package com.pzx.raft.core.entity;

public interface UserDefinedCommandListener {

    void onApply(UserDefinedCommand userDefinedCommand);

    void onWrite(UserDefinedCommand userDefinedCommand, long index, long term);
}
