package com.pzx.rpc.service.handler;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.enumeration.ResponseCode;
import com.pzx.rpc.service.provider.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 利用RpcRequest对应的Service实例，调用相应函数获得结果，并构造RpcResponse
 */
public class ServiceRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestHandler.class);

    public RpcResponse handle(RpcRequest rpcRequest, ServiceProvider serviceProvider) {
        String interfaceName = rpcRequest.getInterfaceName();
        Object service = serviceProvider.getService(interfaceName);
        if (service == null){
            logger.error("未找到对应服务：" + rpcRequest);
            return RpcResponse.fail(rpcRequest.getRequestId(), ResponseCode.CLASS_NOT_FOUND);
        }
        return invokeTargetMethod(rpcRequest, service);
    }

    private RpcResponse invokeTargetMethod(RpcRequest rpcRequest, Object service){

        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            Object result = method.invoke(service, rpcRequest.getParameters());
            logger.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
            return RpcResponse.success(rpcRequest.getRequestId(), result);
        }catch (NoSuchMethodException e){
            logger.error("未找到对应方法：" + rpcRequest + ":" + e);
            return RpcResponse.fail(rpcRequest.getRequestId(), ResponseCode.METHOD_NOT_FOUND, e.toString());
        }catch (IllegalAccessException | InvocationTargetException e){
            logger.error("服务调用时出错：" + rpcRequest + ":" + e);
            return RpcResponse.fail(rpcRequest.getRequestId(), ResponseCode.METHOD_INVOKER_FAIL, e.toString());
        }

    }

}
