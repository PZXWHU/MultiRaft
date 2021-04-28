package com.pzx.raft.core.entity;

public class SMCommand extends Command<byte[], byte[]> {
    public SMCommand() {
    }

    public SMCommand(byte[] key, byte[] value) {
        super(key, value);
    }
}
