package com.pzx.raft.kv.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreConfig {

    private int raftGroupSize = 3;

    private String rocksDBPath;

    private String storeHome;

    private String storeAddress;

    // 与其他节点进行同步、选主等操作的线程池大小
    private int executorThreadNum = 10;


}
