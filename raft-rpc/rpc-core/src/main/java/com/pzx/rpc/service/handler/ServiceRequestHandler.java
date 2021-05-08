package com.pzx.rpc.service.handler;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.service.provider.ServiceProvider;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ServiceRequestHandler {

    RpcResponse handle(RpcRequest rpcRequest);

    CompletableFuture<RpcResponse> handleSync(RpcRequest rpcRequest);

}
