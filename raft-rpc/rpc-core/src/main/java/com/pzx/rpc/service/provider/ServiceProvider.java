package com.pzx.rpc.service.provider;

/**
 * 服务提供接口
 */
public interface ServiceProvider {

    <T> void addService(T service, String serviceName);

    Object getService(String serviceName);

}
