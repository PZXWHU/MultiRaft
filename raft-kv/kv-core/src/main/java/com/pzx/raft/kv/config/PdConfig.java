package com.pzx.raft.kv.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PdConfig {

    private boolean fake;

    private String pdAddress;

}
