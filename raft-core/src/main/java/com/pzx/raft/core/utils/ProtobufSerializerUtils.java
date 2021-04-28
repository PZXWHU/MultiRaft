package com.pzx.raft.core.utils;

import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import io.protostuff.ProtostuffIOUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ProtobufSerializerUtils {

    private static Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static byte[] serialize(Object obj) {
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        Class clazz = obj.getClass();
        Schema schema = getSchema(clazz);
        byte[] data;
        try {
            data = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    public static Object deserialize(byte[] bytes, Class<?> clazz) {
        Schema schema = getSchema(clazz);
        Object obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }

    @SuppressWarnings("unchecked")
    private static Schema getSchema(Class clazz) {
        Schema schema = schemaCache.get(clazz);
        if (Objects.isNull(schema)) {
            // 这个schema通过RuntimeSchema进行懒创建并缓存
            // 所以可以一直调用RuntimeSchema.getSchema(),这个方法是线程安全的
            schema = RuntimeSchema.getSchema(clazz);
            if (Objects.nonNull(schema)) {
                schemaCache.put(clazz, schema);
            }
        }
        return schema;
    }
}
