package com.pzx.rpc.transport;

import com.pzx.rpc.enumeration.SerDeCode;

public interface RpcServer {

    int DEFAULT_SERDE_CODE = SerDeCode.valueOf("KRYO").getCode();

    void start();

    /**
     * 发布服务：包括本地提供服务和注册中心注册服务（如果使用注册中心的话）
     * @param service
     * @param serviceName
     * @param <T>
     */
    <T> void publishService(Object service, String serviceName);
}
