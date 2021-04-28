package com.pzx.raft.core.entity;


public class ConfigCommand extends Command<String, Object> {
    public ConfigCommand() {
    }

    public ConfigCommand(String key, Object value) {
        super(key, value);
    }
}
