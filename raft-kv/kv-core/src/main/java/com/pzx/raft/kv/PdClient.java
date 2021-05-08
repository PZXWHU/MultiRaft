package com.pzx.raft.kv;

import com.pzx.raft.kv.config.PdClientConfig;
import com.pzx.raft.kv.entity.Region;
import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.StoreInfo;
import com.pzx.raft.kv.service.PlaceDriverService;
import com.pzx.raft.kv.service.entity.RegionHeartbeatRequest;
import com.pzx.raft.kv.service.entity.StoreHeartbeatRequest;
import com.pzx.rpc.context.RpcInvokeContext;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.invoke.RpcResponseCallBack;
import com.pzx.rpc.service.proxy.ProxyConfig;

import java.util.List;

public class PdClient {

    private PdClientConfig pdClientConfig;

    private PlaceDriverService placeDriverServiceSync;

    private PlaceDriverService placeDriverServiceAsync;

    public PdClient(PdClientConfig pdClientConfig) {

        this.pdClientConfig = pdClientConfig;

        ProxyConfig proxyConfig = new ProxyConfig()
                .setDirectServerUrl(pdClientConfig.getServerAddress())
                .setInvokeType(InvokeType.SYNC)
                .setTimeout(20000);
        this.placeDriverServiceSync = proxyConfig.getProxy(PlaceDriverService.class);
        proxyConfig.setInvokeType(InvokeType.CALLBACK);
        this.placeDriverServiceAsync = proxyConfig.getProxy(PlaceDriverService.class);
    }

    public Long createRegionId(){
        return placeDriverServiceSync.createRegionId();
    }

    public void sendStoreHeartbeat(StoreHeartbeatRequest storeHeartbeatRequest, RpcResponseCallBack rpcResponseCallBack){
        RpcInvokeContext.getContext().setResponseCallBack(rpcResponseCallBack);
        placeDriverServiceAsync.storeHeartbeat(storeHeartbeatRequest);

    }

    public void sendRegionHeartbeat(RegionHeartbeatRequest regionHeartbeatRequest, RpcResponseCallBack rpcResponseCallBack){
        RpcInvokeContext.getContext().setResponseCallBack(rpcResponseCallBack);
        placeDriverServiceAsync.regionHeartbeat(regionHeartbeatRequest);
    }

    public StoreInfo getStoreInfo(long storeId){
        return placeDriverServiceSync.getStoreInfo(storeId);
    }

    public RegionInfo getRegionInfo(long regionId){
        return placeDriverServiceSync.getRegionInfo(regionId);
    }

    public void setStoreInfo(StoreInfo storeInfo){
        placeDriverServiceSync.setStoreInfo(storeInfo);
    }

    public void setRegionInfo(RegionInfo regionInfo){
        placeDriverServiceSync.setRegionInfo(regionInfo);
    }

    public RegionInfo findRegionByKey(byte[] key) {
        return placeDriverServiceSync.findRegionByKey(key);
    }

    public List<RegionInfo> findRegionsByKeyRange(byte[] startKey, byte[] endKey) {
        return placeDriverServiceSync.findRegionsByKeyRange(startKey, endKey);
    }

}
