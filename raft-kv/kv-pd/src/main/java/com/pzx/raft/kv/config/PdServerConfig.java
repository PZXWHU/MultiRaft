package com.pzx.raft.kv.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PdServerConfig {

    private String serverAddress;

    private String rocksDBPath;

    private long maxSizePerRegion = 6000;

}
