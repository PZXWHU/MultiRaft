package com.pzx.raft.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public interface AsyncKVStore extends KVStore {

    Logger logger = LoggerFactory.getLogger(AsyncKVStore.class);

    default byte[] get(byte[] key){
        try {
            return getAsync(key).get();
        }catch (InterruptedException | ExecutionException e){
            logger.warn(e.getMessage());
        }
        return null;
    }

    default boolean put(byte[] key, byte[] value){
        try {
            return putAsync(key, value).get();
        }catch (InterruptedException | ExecutionException e){
            logger.warn(e.getMessage());
        }
        return false;
    }

    default boolean delete(byte[] key){
        try {
            return deleteAsync(key).get();
        }catch (InterruptedException | ExecutionException e){
            logger.warn(e.getMessage());
        }
        return false;
    }

    default List<byte[]> scan(byte[] startKey, byte[] endKey){
        try {
            return scanAsync(startKey, endKey).get();
        }catch (InterruptedException | ExecutionException e){
            logger.warn(e.getMessage());
        }
        return Collections.emptyList();
    }

    CompletableFuture<byte[]> getAsync(byte[] key);

    CompletableFuture<Boolean> putAsync(byte[] key, byte[] value);

    CompletableFuture<Boolean> deleteAsync(byte[] key);

    CompletableFuture<List<byte[]>> scanAsync(byte[] startKey, byte[] endKey);

}
