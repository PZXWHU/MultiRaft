package com.pzx.raft.kv;

import com.pzx.raft.kv.config.PdConfig;

public class PdClient {

    private PdConfig pdConfig;

    public PdClient(PdConfig pdConfig) {
        this.pdConfig = pdConfig;
    }
}
