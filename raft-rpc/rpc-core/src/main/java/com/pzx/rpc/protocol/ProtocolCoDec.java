package com.pzx.rpc.protocol;

import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcException;
import com.pzx.rpc.factory.SingletonFactory;
import com.pzx.rpc.serde.RpcSerDe;
import io.netty.buffer.ByteBuf;

public interface ProtocolCoDec {

    byte[] encode(Object msg, RpcSerDe serDe);

    Object decode(byte[] bytes);

    int getVersion();

    static ProtocolCoDec getByVersion(byte version){
        switch (version){
            case 1:
                return SingletonFactory.getInstance(ProtocolCoDec1.class);
            default:
                throw new RpcException(RpcError.UNKNOWN_PROTOCOL, "the version is larger than newest version");
        }
    }

}
