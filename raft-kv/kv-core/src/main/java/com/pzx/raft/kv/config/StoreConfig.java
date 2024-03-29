package com.pzx.raft.kv.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreConfig {

    private long storeId;

    private String storeHome;

    private String serverAddress;

    // 与其他节点进行同步、选主等操作的线程池大小
    private int executorThreadNum = 10;

    private int storeHeartbeatPeriodMilliseconds = 10000;

    private int regionHeartbeatPeriodMilliseconds = 10000;

}
