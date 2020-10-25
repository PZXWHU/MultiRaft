package com.pzx.rpc.invoke;

import com.pzx.rpc.context.AsyncRuntime;
import com.pzx.rpc.context.RpcInvokeContext;
import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.enumeration.ResponseCode;
import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcException;
import com.pzx.rpc.factory.ThreadPoolFactory;
import com.pzx.rpc.transport.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CallbackInvoker extends AbstractInvoker {

    private final static Logger logger = LoggerFactory.getLogger(CallbackInvoker.class);

    public CallbackInvoker(RpcClient rpcClient, long timeout) {
        super(rpcClient, timeout);
    }

    @Override
    protected RpcResponse doInvoke(RpcRequest rpcRequest) {

        RpcResponseCallBack rpcResponseCallBack = RpcInvokeContext.getContext().getResponseCallback();
        RpcInvokeContext.getContext().setResponseCallback(null);//将callback赋值为null，避免被下一次rpc所使用。

        CompletableFuture<RpcResponse> resultFuture =  rpcClient.sendRequest(rpcRequest);
        //CallbackInvoke ： 为resultFuture设置callback
        if (rpcResponseCallBack != null){
            resultFuture.whenCompleteAsync((rpcResponse, throwable)->{
                checkRpcServerError(rpcRequest, rpcResponse);
                if (throwable != null)
                    rpcResponseCallBack.onException(throwable);
                else if(rpcResponse.getStatusCode() == ResponseCode.METHOD_INVOKER_SUCCESS.getCode())
                    rpcResponseCallBack.onResponse(rpcResponse.getData());
                else
                    rpcResponseCallBack.onException(new RpcException(RpcError.RPC_INVOKER_FAILED, rpcResponse.getMessage()));
            }, AsyncRuntime.getAsyncThreadPool());
        }

        setTimeoutCheckAsync(rpcRequest, timeout);

        return RpcResponse.EMPTY_RESPONSE;
    }
}
