package com.pzx.rpc.invoke;

import com.pzx.rpc.context.AsyncRuntime;
import com.pzx.rpc.context.RpcInvokeContext;
import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.transport.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneWayInvoker extends AbstractInvoker {

    private static final Logger logger = LoggerFactory.getLogger(OneWayInvoker.class);

    public OneWayInvoker(RpcClient rpcClient, long timeout) {
        super(rpcClient, timeout);
    }

    @Override
    protected RpcResponse doInvoke(RpcRequest rpcRequest) {

        rpcClient.sendRequest(rpcRequest);
        //因为rpcClient.sendRequest会将Future加入RpcInvokeContext.uncompletedFutures
        RpcInvokeContext.removeUncompletedFuture(rpcRequest.getRequestId());

        return RpcResponse.EMPTY_RESPONSE;
    }
}
