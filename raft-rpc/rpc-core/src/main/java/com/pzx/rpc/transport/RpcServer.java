package com.pzx.rpc.transport;

import com.pzx.rpc.enumeration.SerDeCode;

public interface RpcServer {

    int DEFAULT_SERDE_CODE = SerDeCode.PROTOBUF.getCode();

    void start();

    /**
     * 发布服务：包括本地提供服务和注册中心注册服务（如果使用注册中心的话）
     * @param service
     * @param serviceName
     */
    void publishService(Object service, String serviceName);

    /**
     * 发布服务：包括本地提供服务和注册中心注册服务（如果使用注册中心的话）
     * @param service
     */
    default void publishService(Object service){
        Class clazz = service.getClass();
        for(Class c : clazz.getInterfaces()){
            publishService(service, c.getCanonicalName());
        }
    }
}
