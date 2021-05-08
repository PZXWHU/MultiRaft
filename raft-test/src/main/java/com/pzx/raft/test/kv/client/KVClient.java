package com.pzx.raft.test.kv.client;

import com.pzx.raft.core.entity.KVOperation;
import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.kv.PdClient;
import com.pzx.raft.kv.config.PdClientConfig;
import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.service.MultiRaftKVClientService;
import com.pzx.raft.kv.service.entity.MultiRaftKVClientRequest;
import com.pzx.raft.kv.service.entity.MultiRaftKVClientResponse;
import com.pzx.rpc.context.RpcInvokeContext;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.invoke.RpcResponseCallBack;
import com.pzx.rpc.service.proxy.ProxyConfig;

public class KVClient {

    public static void main(String[] args) {
        PdClientConfig pdClientConfig = new PdClientConfig();
        pdClientConfig.setServerAddress("127.0.0.1:8888");
        PdClient pdClient = new PdClient(pdClientConfig);

        ProxyConfig proxyConfig = new ProxyConfig().setInvokeType(InvokeType.CALLBACK).setTimeout(10000);

        RpcResponseCallBack rpcResponseCallBack = new RpcResponseCallBack() {
            @Override
            public void onResponse(Object data) {
                MultiRaftKVClientResponse response = (MultiRaftKVClientResponse)data;
                System.out.println(response);
                if (response.getResult() != null){
                    System.out.println(ByteUtils.bytesToInteger(response.getResult()));
                }
            }

            @Override
            public void onException(Throwable throwable) {
                System.out.println(throwable);
            }
        };

        set(proxyConfig, pdClient, rpcResponseCallBack);

        //get(proxyConfig, pdClient, rpcResponseCallBack);

    }

    public static void set(ProxyConfig proxyConfig, PdClient pdClient, RpcResponseCallBack rpcResponseCallBack ){
        for(int i = 10001; i <= 15000; i++){
            byte[] key = ByteUtils.integerToBytes(i);
            byte[] value = ByteUtils.integerToBytes(i);
            RegionInfo regionInfo = pdClient.findRegionByKey(key);
            String url = regionInfo.getRegion().getRaftGroupAddress().get(regionInfo.getLeaderId());
            proxyConfig.setDirectServerUrl(url);
            MultiRaftKVClientService multiRaftKVClientService = proxyConfig.getProxy(MultiRaftKVClientService.class);
            MultiRaftKVClientRequest multiRaftKVClientRequest = new MultiRaftKVClientRequest();
            multiRaftKVClientRequest.setRegionId(regionInfo.getRegion().getRegionId());
            multiRaftKVClientRequest.setRegionEpoch(regionInfo.getRegion().getRegionEpoch());
            multiRaftKVClientRequest.setKey(key);
            multiRaftKVClientRequest.setValue(value);
            multiRaftKVClientRequest.setKvOperation(KVOperation.PUT);
            RpcInvokeContext.getContext().setResponseCallBack(rpcResponseCallBack);
            multiRaftKVClientService.operateKV(multiRaftKVClientRequest);
        }
    }

    public static void get(ProxyConfig proxyConfig, PdClient pdClient, RpcResponseCallBack rpcResponseCallBack ){
        for(int i = 1; i <= 10000; i++){
            byte[] key = ByteUtils.integerToBytes(i);
            RegionInfo regionInfo = pdClient.findRegionByKey(key);
            System.out.println(regionInfo);
            String url = regionInfo.getRegion().getRaftGroupAddress().get(regionInfo.getLeaderId());
            proxyConfig.setDirectServerUrl(url);
            MultiRaftKVClientService multiRaftKVClientService = proxyConfig.getProxy(MultiRaftKVClientService.class);
            MultiRaftKVClientRequest multiRaftKVClientRequest = new MultiRaftKVClientRequest();
            multiRaftKVClientRequest.setRegionId(regionInfo.getRegion().getRegionId());
            multiRaftKVClientRequest.setRegionEpoch(regionInfo.getRegion().getRegionEpoch());
            multiRaftKVClientRequest.setKey(key);
            multiRaftKVClientRequest.setKvOperation(KVOperation.GET);
            RpcInvokeContext.getContext().setResponseCallBack(rpcResponseCallBack);
            multiRaftKVClientService.operateKV(multiRaftKVClientRequest);
        }
    }

}
