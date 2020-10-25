package com.pzx.rpc.service.provider;

import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryServiceProvider implements ServiceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MemoryServiceProvider.class);
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    //private final Set<String> registeredServiceName = ConcurrentHashMap.newKeySet();

    @Override
    public <T> void addService(T service, String serviceName) {
        if (!serviceMap.containsKey(serviceName)){
            logger.info("add service provider：serviceName:{}, " +
                    "serviceImplClass: {}, serviceObject: {}", serviceName, service.getClass().getCanonicalName(),service.toString());
        }else {
            logger.info("update service provider：serviceName:{}, " +
                            "oldServiceImplClass: {}, OldServiceObject: {}, " +
                            "newServiceImplClass: {}, newServiceObject: {}",
                    serviceName, serviceMap.get(serviceName).getClass().getCanonicalName(),serviceMap.get(serviceName).toString(),
                    service.getClass().getCanonicalName(), service.toString());
        }
        serviceMap.put(serviceName,service);
        /*
        String serviceName = service.getClass().getCanonicalName();

        //contains和add会有并发问题：某个线程在另一个线程add还未添加成功之前，判断contains==false，造成同一个服务类对象注册多次。
        synchronized (registeredServiceName){
            if(registeredServiceName.contains(serviceName)) return;//避免同一个服务接口实现类的服务对象实例注册两次
            registeredServiceName.add(serviceName);
        }

        Class<?>[] interfaces = service.getClass().getInterfaces();
        if(interfaces.length == 0){
            throw new RpcException(RpcError.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
        }
        for(Class<?> i : interfaces){
            serviceMap.put(i.getCanonicalName(), service);
        }
        logger.info("服务接口: {} 注册服务实例: {}", interfaces, serviceName);
         */
    }

    @Override
    public Object getService(String serviceName) {
        Object service = serviceMap.get(serviceName);
        return service;

    }
}
