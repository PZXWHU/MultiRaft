package com.pzx.rpc.service.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcException;
import com.pzx.rpc.factory.SingletonFactory;
import com.pzx.rpc.service.balancer.LoadBalancer;
import com.pzx.rpc.service.balancer.RandomBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.jca.GetInstance;
import sun.security.provider.certpath.PKIXTimestampParameters;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NacosServiceRegistry extends AbstractServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NacosServiceRegistry.class);

    //private final InetSocketAddress registryCenterAddress;
    private final NamingService namingService;
    private final LoadBalancer loadBalancer;

    public NacosServiceRegistry(InetSocketAddress registryCenterAddress){
        this(registryCenterAddress, SingletonFactory.getInstance(RandomBalancer.class));
    }

    public NacosServiceRegistry(InetSocketAddress registryCenterAddress, LoadBalancer loadBalancer){
        //this.registryCenterAddress = registryCenterAddress;
        this.loadBalancer = loadBalancer;
        String address = registryCenterAddress.getAddress().getHostAddress() + ":" + registryCenterAddress.getPort();
        try {
            this.namingService = NamingFactory.createNamingService(address);
        } catch (NacosException e) {
            logger.error("连接到Nacos时有错误发生: ", e);
            throw new RpcException(RpcError.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
        }

        //JVM关闭前，将所有注册的服务注销
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.clearRegisteredService();
        }));
    }

    @Override
    public void registerService(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            namingService.registerInstance(serviceName, inetSocketAddress.getHostName(), inetSocketAddress.getPort());
            registeredService.put(serviceName, inetSocketAddress);
        } catch (NacosException e) {
            logger.error("注册服务时有错误发生:", e);
            //throw new RpcException(RpcError.REGISTER_SERVICE_FAILED);
        }
    }

    @Override
    public void deregisterService(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            namingService.deregisterInstance(serviceName, inetSocketAddress.getHostName(), inetSocketAddress.getPort());
            registeredService.remove(serviceName, inetSocketAddress);
        } catch (NacosException e) {
            logger.error("注销服务时有错误发生:", e);
            //throw new RpcException(RpcError.DEREGISTER_SERVICE_FAILED);
        }
    }

    @Override
    public InetSocketAddress lookupService(String serviceName) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName);
            Instance instance = instances.get(0);

            return new InetSocketAddress(instance.getIp(), instance.getPort());
        } catch (NacosException e) {
            logger.error("获取服务时有错误发生:", e);
        }
        return null;
    }

}
