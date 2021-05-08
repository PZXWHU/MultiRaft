package com.pzx.raft.test.core.log;

import com.pzx.raft.core.entity.KVOperation;
import com.pzx.raft.core.service.RaftKVClientService;
import com.pzx.raft.core.service.entity.KVClientRequest;
import com.pzx.raft.core.service.entity.KVClientResponse;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.service.proxy.ProxyConfig;

import java.util.concurrent.CompletableFuture;

public class RaftNodeReplicateLog1 {

    public static void main(String[] args) throws Exception {


        ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:8001").setInvokeType(InvokeType.SYNC);
        RaftKVClientService raftKVClientService = proxyConfig.getProxy(RaftKVClientService.class);

        for (int i = 0; i < 100; i++){
            long t = System.currentTimeMillis();
            Object o = raftKVClientService.operateKV(KVClientRequest.builder()
                    .kvOperation(KVOperation.PUT)
                    .key(ByteUtils.integerToBytes(i))
                    .value(ByteUtils.integerToBytes(i)).build());
            CompletableFuture<KVClientResponse> future = (CompletableFuture<KVClientResponse>)o;
            System.out.println("消耗时间：" + (System.currentTimeMillis() - t) +  "  " +  future.get());
        }

    }

}
