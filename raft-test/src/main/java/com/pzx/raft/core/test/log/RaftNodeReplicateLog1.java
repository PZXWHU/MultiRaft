package com.pzx.raft.core.test.log;

import com.pzx.raft.core.service.RaftKVClientService;
import com.pzx.raft.core.service.entity.ClientKVRequest;
import com.pzx.raft.core.service.entity.ClientKVResponse;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.service.proxy.ProxyConfig;

public class RaftNodeReplicateLog1 {

    public static void main(String[] args) {


        ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:8001").setInvokeType(InvokeType.SYNC);
        RaftKVClientService raftKVClientService = proxyConfig.getProxy(RaftKVClientService.class);

        for (int i = 0; i < 100; i++){
            long t = System.currentTimeMillis();
            ClientKVResponse clientKVResponse = raftKVClientService.operateKV(ClientKVRequest.builder()
                    .type(ClientKVRequest.PUT)
                    .key(ByteUtils.integerToBytes(i))
                    .value(ByteUtils.integerToBytes(i)).build());
            System.out.println("消耗时间：" + (System.currentTimeMillis() - t) +  "  " +  clientKVResponse);
        }

    }

}
