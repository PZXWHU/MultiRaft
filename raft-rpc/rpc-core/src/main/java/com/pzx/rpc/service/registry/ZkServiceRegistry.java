package com.pzx.rpc.service.registry;

import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcException;
import com.pzx.rpc.factory.SingletonFactory;
import com.pzx.rpc.service.balancer.LoadBalancer;
import com.pzx.rpc.service.balancer.RandomBalancer;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

public class ZkServiceRegistry extends AbstractServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZkServiceRegistry.class);
    private static final String RPC_BASE_PATH = "rpc";


    private final LoadBalancer loadBalancer;
    private final ServiceDiscovery serviceDiscovery;

    public ZkServiceRegistry(InetSocketAddress registryCenterAddress){
        this(registryCenterAddress, SingletonFactory.getInstance(RandomBalancer.class));
    }


    public ZkServiceRegistry(InetSocketAddress registryCenterAddress, LoadBalancer loadBalancer){

        this.loadBalancer = loadBalancer;
        String connectString = registryCenterAddress.getAddress().getHostAddress() + ":" + registryCenterAddress.getPort();
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(connectString,retryPolicy);
        client.start();
        this.serviceDiscovery = ServiceDiscoveryBuilder.builder(Void.class)
                .client(client)
                .basePath(RPC_BASE_PATH)
                .build();

    }

    @Override
    public void registerService(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            ServiceInstance serviceInstance = ServiceInstance.builder()
                    .address(inetSocketAddress.getAddress().getHostAddress())
                    .port(inetSocketAddress.getPort())
                    .build();
            serviceDiscovery.registerService(serviceInstance);
            registeredService.put(serviceName, inetSocketAddress);
        }catch (Exception e){
            logger.error("注册服务时有错误发生：" + e);
            throw new RpcException(RpcError.REGISTER_SERVICE_FAILED);
        }

    }

    @Override
    public void deregisterService(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            ServiceInstance serviceInstance = ServiceInstance.builder()
                    .address(inetSocketAddress.getAddress().getHostAddress())
                    .port(inetSocketAddress.getPort())
                    .build();
            serviceDiscovery.unregisterService(serviceInstance);
            registeredService.remove(serviceName, inetSocketAddress);
        }catch (Exception e){
            logger.error("注销服务时有错误发生 ：" + e);
            throw new RpcException(RpcError.DEREGISTER_SERVICE_FAILED);
        }
    }

    @Override
    public InetSocketAddress lookupService(String serviceName) {
        try {
            Collection<ServiceInstance> instances = serviceDiscovery.queryForInstances(serviceName);
            List<ServiceInstance> instanceList = new ArrayList(instances);
            ServiceInstance instance = loadBalancer.select(instanceList);
            return new InetSocketAddress(instance.getAddress(), instance.getPort());
        }catch (Exception e){
            logger.warn("获取服务时有错误发生:" + e);
        }
        return null;
    }

}
