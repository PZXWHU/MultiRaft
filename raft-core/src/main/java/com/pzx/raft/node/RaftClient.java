package com.pzx.raft.node;

import com.pzx.raft.service.RaftKVClientService;
import com.pzx.raft.service.entity.ClientKVRequest;
import com.pzx.raft.service.entity.ClientKVResponse;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.service.proxy.ProxyConfig;

public class RaftClient {

    public static void main(String[] args) {
        ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:7999").setInvokeType(InvokeType.SYNC);
        RaftKVClientService raftKVClientService = proxyConfig.getProxy(RaftKVClientService.class);
        /*
        for (int i = 0; i < 100; i++){
            long t = System.currentTimeMillis();
            ClientKVResponse clientKVResponse = raftKVClientService.operateKV(ClientKVRequest.builder().type(ClientKVRequest.PUT).key(i + "").value(i).build());
            System.out.println("消耗时间：" + (System.currentTimeMillis() - t) +  "  " +  clientKVResponse);
        }

         */
        for (int i = 0; i < 100; i++){
            long t = System.currentTimeMillis();
            ClientKVResponse clientKVResponse = raftKVClientService.operateKV(ClientKVRequest.builder().type(ClientKVRequest.GET).key(i + "").build());
            System.out.println("消耗时间：" + (System.currentTimeMillis() - t) +  "  " +  clientKVResponse);
        }
    }

}
