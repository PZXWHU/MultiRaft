package com.pzx.rpc.protocol;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcException;
import com.pzx.rpc.serde.RpcSerDe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Protocol Version == 1
 *
 * +--------------+--------------+----------------+---------------+---------------+----------------+
 * | Package Head |Package Length|Protocol Version|  Magic Number |  Package Type | Serializer Type|
 * |              |   4 bytes    |    1 bytes     |     4 byte    |     1 byte    |      1 byte    |
 * +--------------+--------------+--------------- +---------------+---------------+----------------+
 * | Package Body |                          Data Bytes                                            |
 * |              |                   Length: ${Package Length - 11}                               |
 * +-----------------------------------------------------------------------------------------------+
 */
public class ProtocolCoDec1 implements ProtocolCoDec {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolCoDec1.class);
    private static final byte PROTOCOL_VERSION = 1;
    private static final int PACKAGE_HEAD_LENGTH = 11;

    private ProtocolCoDec1(){}

    @Override
    public  byte[] encode(Object msg, RpcSerDe serializer) {

        byte[] msgBytes = serializer.serialize(msg);
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(msgBytes.length + 11);
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)){

            dataOutputStream.writeInt(PACKAGE_HEAD_LENGTH + msgBytes.length);
            dataOutputStream.writeByte(PROTOCOL_VERSION);
            dataOutputStream.writeInt(ProtocolConstants.MAGIC_NUMBER);
            if(msg instanceof RpcRequest) {
                dataOutputStream.writeByte(ProtocolConstants.REQUEST_TYPE);
            } else {
                dataOutputStream.writeByte(ProtocolConstants.RESPONSE_TYPE);
            }
            dataOutputStream.writeByte(serializer.getCode());
            dataOutputStream.write(msgBytes);
            return byteArrayOutputStream.toByteArray();
        }catch (IOException e){
            logger.warn("序列化时发生错误：" + e);
            throw new RpcException(RpcError.SERIALIZE_FAIL);
        }

    }

    @Override
    public Object decode(byte[] bytes) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)){

            int packageLength = dataInputStream.readInt();
            int version = dataInputStream.readByte();
            if (version != PROTOCOL_VERSION){
                throw new RuntimeException("通讯协议解码器选择错误");
            }

            int magic = dataInputStream.readInt();
            if(magic != ProtocolConstants.MAGIC_NUMBER) {
                logger.error("不识别的协议包: {}", magic);
                throw new RpcException(RpcError.UNKNOWN_PROTOCOL,"magic number is error");
            }

            byte packageCode = dataInputStream.readByte();
            Class<?> packageClass;
            if(packageCode == ProtocolConstants.REQUEST_TYPE) {
                packageClass = RpcRequest.class;
            }else if(packageCode == ProtocolConstants.RESPONSE_TYPE) {
                packageClass = RpcResponse.class;
            } else {
                logger.error("不识别的数据包: {}", packageCode);
                throw new RpcException(RpcError.UNKNOWN_PACKAGE_TYPE);
            }

            byte serializerCode = dataInputStream.readByte();
            RpcSerDe deserializer = RpcSerDe.getByCode(serializerCode);

            byte[] msg = new byte[packageLength - PACKAGE_HEAD_LENGTH];
            dataInputStream.read(msg);
            Object obj = deserializer.deserialize(msg, packageClass);

            return obj;
        }catch (IOException e){
            logger.error("反序列化时发生错误：" + e);
            throw new RpcException(RpcError.DESERIALIZE_FAIL);
        }

    }

    @Override
    public int getVersion() {
        return PROTOCOL_VERSION;
    }

}
