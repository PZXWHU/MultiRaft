package com.pzx.rpc.transport.netty.codec;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.serde.RpcSerDe;
import com.pzx.rpc.protocol.ProtocolCoDec;
import com.pzx.rpc.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * netty的编码拦截器，将RpcRequest或者RpcResponse编码为通信协议字节数组
 */
public class ProtocolNettyEncoder extends MessageToByteEncoder {

    private final RpcSerDe serializer;

    public ProtocolNettyEncoder(RpcSerDe serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf out) throws Exception {

        byte protocolVersion = ProtocolConstants.NEWEST_VERSION;
        ProtocolCoDec protocolCoDec = ProtocolCoDec.getByVersion(protocolVersion);
        byte[] protocolBytes = protocolCoDec.encode(msg, serializer);
        out.writeBytes(protocolBytes);

    }
}
