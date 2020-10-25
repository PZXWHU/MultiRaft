package com.pzx.rpc.invoke;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.transport.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SyncInvoker extends AbstractInvoker {

    private static final Logger logger = LoggerFactory.getLogger(SyncInvoker.class);

    public SyncInvoker(RpcClient rpcClient, long timeout) {
        super(rpcClient, timeout);
    }

    @Override
    protected RpcResponse doInvoke(RpcRequest rpcRequest) {
        RpcResponse rpcResponse = RpcResponse.EMPTY_RESPONSE;
        CompletableFuture<RpcResponse> resultFuture =  rpcClient.sendRequest(rpcRequest);
        try {
            rpcResponse = resultFuture.get(timeout, TimeUnit.MILLISECONDS);
        }catch (TimeoutException e){
            logger.error("RPC调用超时 ：" + rpcRequest);
        }catch (InterruptedException | ExecutionException e) {
            logger.error("RPC调用失败 ：" + e);
        }
        checkRpcServerError(rpcRequest, rpcResponse);
        return rpcResponse;
    }
}
