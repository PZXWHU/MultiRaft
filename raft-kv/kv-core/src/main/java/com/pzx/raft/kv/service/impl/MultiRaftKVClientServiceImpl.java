package com.pzx.raft.kv.service.impl;

import com.pzx.raft.core.entity.KVOperation;
import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.kv.RegionEngine;
import com.pzx.raft.kv.StoreEngine;
import com.pzx.raft.kv.entity.EpochSMCommand;
import com.pzx.raft.kv.service.MultiRaftKVClientService;
import com.pzx.raft.kv.service.entity.MultiRaftKVClientRequest;
import com.pzx.raft.kv.service.entity.MultiRaftKVClientResponse;
import com.pzx.rpc.context.RpcInvokeContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

@Slf4j
public class MultiRaftKVClientServiceImpl implements MultiRaftKVClientService {

    private StoreEngine storeEngine;

    private ConcurrentMap<Long, RegionEngine> regionEngineTable;

    public MultiRaftKVClientServiceImpl(StoreEngine storeEngine) {
        this.storeEngine = storeEngine;
        this.regionEngineTable = storeEngine.getRegionEngineTable();
    }

    @Override
    public MultiRaftKVClientResponse operateKV(MultiRaftKVClientRequest request) {
        CompletableFuture<MultiRaftKVClientResponse> responseCompletableFuture = new CompletableFuture<>();
        MultiRaftKVClientResponse multiRaftKVClientResponse = new MultiRaftKVClientResponse();
        if (!regionEngineTable.containsKey(request.getRegionId())){
            multiRaftKVClientResponse.setMessage("the region is not found ");
            responseCompletableFuture.complete(multiRaftKVClientResponse);
        } else{
            RegionEngine regionEngine = regionEngineTable.get(request.getRegionId());
            if (regionEngine.getRegion().getRegionEpoch() != request.getRegionEpoch()){
                multiRaftKVClientResponse.setMessage("the region is expired ");
                responseCompletableFuture.complete(multiRaftKVClientResponse);
            }else{
                if (!regionEngine.isLeader()){
                    multiRaftKVClientResponse.setMessage("the region is not the leader");
                    responseCompletableFuture.complete(multiRaftKVClientResponse);
                }else{
                    if (request.getKvOperation() == KVOperation.GET){
                       CompletableFuture<byte[]> future = regionEngine.getAsync(request.getKey());
                       future.whenComplete((data, throwable) -> {
                           if (throwable == null){
                               multiRaftKVClientResponse.setSuccess(true);
                               multiRaftKVClientResponse.setResult(data);
                           }else {
                               multiRaftKVClientResponse.setMessage(throwable.getMessage());
                           }
                           responseCompletableFuture.complete(multiRaftKVClientResponse);
                        });
                    }else {
                        CompletableFuture<Boolean> future = regionEngine.putAsync(request.getKey(), request.getValue());
                        future.whenComplete((data, throwable) -> {
                            if (throwable == null){
                                multiRaftKVClientResponse.setSuccess(true);
                            }else {
                                multiRaftKVClientResponse.setMessage(throwable.getMessage());
                            }
                            responseCompletableFuture.complete(multiRaftKVClientResponse);
                        });

                    }
                }
            }
        }
        RpcInvokeContext.getContext().setFuture(responseCompletableFuture);
        return null;
    }
}
