package com.pzx.raft.test.core.log;

import com.pzx.raft.core.entity.KVOperation;
import com.pzx.raft.core.service.RaftKVClientService;
import com.pzx.raft.core.service.entity.KVClientRequest;
import com.pzx.raft.core.service.entity.KVClientResponse;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.service.proxy.ProxyConfig;

public class RaftNodeReplicateLog2 {

    public static void main(String[] args) {
        ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:8001").setInvokeType(InvokeType.SYNC);
        RaftKVClientService raftKVClientService = proxyConfig.getProxy(RaftKVClientService.class);


        for (int i = 0; i < 100; i++){
            long t = System.currentTimeMillis();
            KVClientResponse KVClientResponse = raftKVClientService.operateKV(KVClientRequest.builder()
                    .kvOperation(KVOperation.GET)
                    .key(ByteUtils.integerToBytes(i))
                    .build());
            System.out.println("消耗时间：" + (System.currentTimeMillis() - t) +  "  " +  ByteUtils.bytesToInteger(KVClientResponse.getResult()));
        }

    }

}
