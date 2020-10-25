package com.pzx.rpc.serde;

import com.esotericsoftware.kryo.Kryo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.enumeration.SerDeCode;
import com.pzx.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonSerDe implements RpcSerDe {

    private static final Logger logger = LoggerFactory.getLogger(JsonSerDe.class);

    //ObjectMapper是线程安全的，可以使用单例
    private ObjectMapper objectMapper = new ObjectMapper();

    private JsonSerDe(){}

    @Override
    public byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            logger.error("序列化时有错误发生:", e);
            throw new RpcException(RpcError.SERIALIZE_FAIL);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        try {
            Object obj = objectMapper.readValue(bytes, clazz);
            if(obj instanceof RpcRequest) {
                obj = handleRpcRequest(obj);
            }
            return obj;
        } catch (IOException e) {
            logger.error("反序列化时有错误发生: {}", e.getMessage());
            throw new RpcException(RpcError.DESERIALIZE_FAIL);
        }
    }

    /*
        由于使用JSON序列化和反序列化Object数组，无法保证反序列化后仍然为原实例类型
        需要重新判断处理
     */
    private Object handleRpcRequest(Object obj) throws IOException {
        RpcRequest rpcRequest = (RpcRequest) obj;
        for(int i = 0; i < rpcRequest.getParamTypes().length; i ++) {
            Class<?> clazz = rpcRequest.getParamTypes()[i];
            if(!clazz.isAssignableFrom(rpcRequest.getParameters()[i].getClass())) {
                byte[] bytes = objectMapper.writeValueAsBytes(rpcRequest.getParameters()[i]);
                rpcRequest.getParameters()[i] = objectMapper.readValue(bytes, clazz);
            }
        }
        return rpcRequest;
    }

    @Override
    public byte getCode() {
        return SerDeCode.valueOf("JSON").getCode();
    }
}
