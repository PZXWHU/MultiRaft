package com.pzx.rpc.serde;

import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcException;
import com.pzx.rpc.factory.SingletonFactory;

/**
 * 线程安全的序列化器，序列化RpcRequest和RpcResponse
 */
public interface RpcSerDe {

    byte[] serialize(Object obj);

    Object deserialize(byte[] bytes, Class<?> clazz);

    byte getCode();

    static RpcSerDe getByCode(int code) {
        switch (code) {
            case 0:
                return SingletonFactory.getInstance(KryoSerDe.class);
            case 1:
                return SingletonFactory.getInstance(JsonSerDe.class);
            default:
                throw new RpcException(RpcError.UNKNOWN_SERDE);
        }
    }

}
