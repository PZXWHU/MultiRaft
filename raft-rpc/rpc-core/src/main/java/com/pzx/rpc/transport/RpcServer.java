package com.pzx.rpc.transport;

import com.pzx.rpc.enumeration.SerDeCode;

import java.util.*;

public interface RpcServer {

    int DEFAULT_SERDE_CODE = SerDeCode.PROTOBUF.getCode();

    void start();

    void stop();

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
        Set<String> serviceNameSet = new HashSet<>();
        Deque<Class> stack = new LinkedList<>();
        stack.push(clazz);
        while (!stack.isEmpty()){
            Class popClazz = stack.pop();
            serviceNameSet.add(popClazz.getCanonicalName());
            for(Class c : popClazz.getInterfaces()){
                stack.push(c);
            }
        }
        for(String serviceName : serviceNameSet){
            publishService(service, serviceName);
        }
    }

    static void getAllClazz(Class<?> clazz, List<Class<?>> superClazzList) {
        if (clazz == null) {
            return;
        }
        if (!superClazzList.contains(clazz)) {
            superClazzList.add(clazz);
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> interfaceCls : interfaces) {
                getAllClazz(interfaceCls, superClazzList);
            }
        }
    }

}
