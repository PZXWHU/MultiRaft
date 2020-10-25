package com.pzx.rpc.transport;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.enumeration.SerDeCode;
import io.netty.util.concurrent.CompleteFuture;

import java.util.concurrent.CompletableFuture;

public interface RpcClient {

    int DEFAULT_SERDE_CODE = SerDeCode.valueOf("KRYO").getCode();

    CompletableFuture<RpcResponse> sendRequest(RpcRequest rpcRequest);
}
