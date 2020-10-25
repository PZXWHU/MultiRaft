package com.pzx.rpc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sun.security.provider.certpath.PKIXTimestampParameters;

import java.io.Serializable;

/**
 * rpc客户端将rpc服务端发送的请求对象
 * 包含四个要素：
 * 1、调用接口名称
 * 2、调用方法名称
 * 3、调用方法参数
 * 4、调用方法参数类型
 * 上面四个要素可满足唯一确定方法，并调用方法得到结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcRequest implements Serializable {

    /**
     * 请求号
     */
    private Integer requestId;

    /**
     * 调用接口名称
     */
    private String interfaceName;

    /**
     * 调用方法名称
     */
    private String methodName;

    /**
     * 调用方法的参数
     */
    private Object[] parameters;

    /**
     * 调用方法的参数的类型
     */
    private Class<?>[] paramTypes;//参数类型用字符串也可以

    private static int lastRequestId = -1;
    public synchronized static Integer nextRequestId(){
        if (lastRequestId == Integer.MAX_VALUE)
            lastRequestId = -1;
        return ++lastRequestId;
    }
}
