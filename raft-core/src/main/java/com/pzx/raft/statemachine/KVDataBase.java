package com.pzx.raft.statemachine;

public interface KVDataBase {

    Object get(String key);

    boolean set(String key, Object value);


}
