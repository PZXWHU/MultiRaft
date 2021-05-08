/*
package com.pzx.rpc.invoke;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.transport.RpcClient;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class AsyncInvoker extends AbstractInvoker {

    public AsyncInvoker(RpcClient rpcClient, long timeout) {
        super(rpcClient, timeout);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String interfaceName = method.getDeclaringClass().getCanonicalName();
        if (interfaceName.endsWith("Async"))
            interfaceName = interfaceName.substring(0, interfaceName.length() - 5);

        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId(RpcRequest.nextRequestId())
                .interfaceName(interfaceName)
                .methodName(method.getName())
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .build();
        CompletableFuture<RpcResponse> rpcResponseFuture = rpcClient.sendRequest(rpcRequest);
        CompletableFuture dataFuture = rpcResponseFuture.thenApply(RpcResponse ::getData);
        return dataFuture;
    }

    @Override
    protected RpcResponse doInvoke(RpcRequest rpcRequest) {
        return  null;
    }
}
*/
