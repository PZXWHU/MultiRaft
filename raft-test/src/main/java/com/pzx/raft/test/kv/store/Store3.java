package com.pzx.raft.test.kv.store;

import com.pzx.raft.kv.StoreEngine;
import com.pzx.raft.kv.config.PdClientConfig;
import com.pzx.raft.kv.config.StoreConfig;

public class Store3 {
    public static void main(String[] args) {
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setStoreId(3);
        storeConfig.setServerAddress("127.0.0.1:8003");
        storeConfig.setStoreHome("C:\\Users\\PZX\\Desktop\\raft\\StoreHome3");

        PdClientConfig pdClientConfig = new PdClientConfig();
        pdClientConfig.setServerAddress("127.0.0.1:8888");
        StoreEngine storeEngine = new StoreEngine(storeConfig, pdClientConfig);

        storeEngine.start();
    }
}
