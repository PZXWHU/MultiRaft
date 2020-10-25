package com.pzx.rpc.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum  RpcError {

    RPC_INVOKER_FAILED("Rpc调用失败"),
    RPC_INVOKER_TIMEOUT("Rpc调用超时"),
    REGISTER_SERVICE_FAILED("注册服务失败"),
    DEREGISTER_SERVICE_FAILED("注册服务失败"),
    FAILED_TO_CONNECT_TO_SERVICE_REGISTRY("无法连接到注册中心"),
    UNKNOWN_ERROR("未知错误"),
    SERVICE_SCAN_PACKAGE_NOT_FOUND("启动类缺少 @ServiceScan 注解"),
    UNKNOWN_SERDE("不识别的的序列化器/反序列化器"),
    UNKNOWN_PACKAGE_TYPE("不识别的数据包"),
    UNKNOWN_PROTOCOL("不识别的通信协议"),
    SERIALIZE_FAIL("序列化失败"),
    DESERIALIZE_FAIL("反序列化失败"),
    SERVICE_NOT_FOUND("未找到对应的服务"),
    SERVICE_NOT_IMPLEMENT_ANY_INTERFACE("注册的服务未实现接口");

    private final String message;
}
