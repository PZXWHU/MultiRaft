package com.pzx.raft.test.kv.store;

import com.pzx.raft.kv.StoreEngine;
import com.pzx.raft.kv.config.PdClientConfig;
import com.pzx.raft.kv.config.StoreConfig;

public class Store1 {

    public static void main(String[] args) {

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setStoreId(1);
        storeConfig.setServerAddress("127.0.0.1:8001");
        storeConfig.setStoreHome("C:\\Users\\PZX\\Desktop\\raft\\StoreHome1");

        PdClientConfig pdClientConfig = new PdClientConfig();
        pdClientConfig.setServerAddress("127.0.0.1:8888");
        StoreEngine storeEngine = new StoreEngine(storeConfig, pdClientConfig);

        storeEngine.start();

    }

}
