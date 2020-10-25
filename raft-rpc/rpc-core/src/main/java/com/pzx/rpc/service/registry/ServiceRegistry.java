package com.pzx.rpc.service.registry;

import java.net.InetSocketAddress;

public interface ServiceRegistry {

    void registerService(String serviceName, InetSocketAddress inetSocketAddress);

    void deregisterService(String serviceName, InetSocketAddress inetSocketAddress);

    InetSocketAddress lookupService(String serviceName);

}
