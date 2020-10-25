package com.pzx.rpc.invoke;

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractInvoker implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractInvoker.class);

    protected final RpcClient rpcClient;
    protected final long timeout;

    public AbstractInvoker(RpcClient rpcClient, long timeout){
        this.rpcClient = rpcClient;
        this.timeout = timeout;
    }

    /**
     * Rpc调用，如果调用失败，则返回null
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId(RpcRequest.nextRequestId())
                .interfaceName(method.getDeclaringClass().getCanonicalName())
                .methodName(method.getName())
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .build();
        RpcResponse rpcResponse = doInvoke(rpcRequest);
        return rpcResponse.getData();
    }

    abstract protected RpcResponse doInvoke(RpcRequest rpcRequest);

    protected static void checkRpcServerError(RpcRequest rpcRequest, RpcResponse rpcResponse){
        if (rpcResponse != null && rpcResponse != RpcResponse.EMPTY_RESPONSE
                && rpcResponse.getStatusCode() != ResponseCode.METHOD_INVOKER_SUCCESS.getCode()){
            logger.error("Rpc调用失败：{} : RpcServer出现错误 :{}", rpcRequest, rpcResponse.getMessage());
        }
    }

    protected static void setTimeoutCheckAsync(RpcRequest rpcRequest, long timeout){
        ////超时清除resultFuture
        ThreadPoolFactory.getScheduledThreadPool().schedule(()->{
            CompletableFuture completableFuture;
            if ((completableFuture = RpcInvokeContext.removeUncompletedFuture(rpcRequest.getRequestId())) != null){
                completableFuture.completeExceptionally(new RpcException(RpcError.RPC_INVOKER_TIMEOUT));
                logger.error("Rpc调用超时：" + rpcRequest);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

}
