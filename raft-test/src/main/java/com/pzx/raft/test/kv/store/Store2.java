package com.pzx.raft.test.kv.store;

import com.pzx.raft.kv.StoreEngine;
import com.pzx.raft.kv.config.PdClientConfig;
import com.pzx.raft.kv.config.StoreConfig;

public class Store2 {

    public static void main(String[] args) {
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setStoreId(2);
        storeConfig.setServerAddress("127.0.0.1:8002");
        storeConfig.setStoreHome("C:\\Users\\PZX\\Desktop\\raft\\StoreHome2");

        PdClientConfig pdClientConfig = new PdClientConfig();
        pdClientConfig.setServerAddress("127.0.0.1:8888");
        StoreEngine storeEngine = new StoreEngine(storeConfig, pdClientConfig);

        storeEngine.start();
    }

}
