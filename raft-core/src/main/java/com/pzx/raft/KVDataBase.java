package com.pzx.raft;

public interface KVDataBase {

    /**
     *
     * @param key
     * @return Returns the value to which the specified key is mapped,
     *         or {@code null} if this map contains no mapping for the key.
     */
    Object get(String key);

    /**
     *
     * @param key
     * @param value
     */
    boolean set(String key, Object value);

    /**
     *
     * @param key
     */
    boolean delete(String key);

}
