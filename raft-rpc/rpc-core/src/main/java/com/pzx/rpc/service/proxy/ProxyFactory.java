package com.pzx.rpc.service.proxy;

public class ProxyFactory {

    private final ProxyConfig proxyConfig;

    public ProxyFactory(ProxyConfig proxyConfig){
        this.proxyConfig = new ProxyConfig();
        proxyConfig.setInvokeType(proxyConfig.getInvokeType())
                .setTimeout(proxyConfig.getTimeout())
                .setDirectServerUrl(proxyConfig.getDirectServerUrl())
                .setRegistryCenterUrl(proxyConfig.getRegistryCenterUrl());
    }

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz){
        return proxyConfig.getProxy(clazz);
    }





}
