package com.pzx.raft.test.kv.pd;

import com.pzx.raft.kv.PlaceDriverServer;
import com.pzx.raft.kv.config.PdServerConfig;

public class PdServerTest {

    public static void main(String[] args) {
        PdServerConfig pdServerConfig = new PdServerConfig();
        pdServerConfig.setRocksDBPath("C:\\Users\\PZX\\Desktop\\raft\\PDHome");
        pdServerConfig.setServerAddress("127.0.0.1:8888");

        PlaceDriverServer placeDriverServer = new PlaceDriverServer(pdServerConfig);

        placeDriverServer.start();
    }

}
