package com.pzx.rpc.service.registry;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractServiceRegistry implements ServiceRegistry {

    protected final Map<String, InetSocketAddress> registeredService;

    {
        registeredService = new HashMap<>();
        //JVM关闭前，将所有注册的服务注销
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.clearRegisteredService();
        }));
    }

    public void clearRegisteredService() {
        for(Map.Entry<String, InetSocketAddress> entry : registeredService.entrySet()){
            deregisterService(entry.getKey(), entry.getValue());
            System.out.println("注销服务");
        }
        registeredService.clear();
    }
}
