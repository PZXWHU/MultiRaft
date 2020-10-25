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
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     */
    Object set(String key, Object value);

    /**
     *
     * @param key
     * @return the previous value associated with {@code key}, or
     *         @code null} if there was no mapping for {@code key}
     */
    Object delete(String key);

}
