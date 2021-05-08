package com.pzx.raft.kv.meta;

import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.core.utils.ProtobufSerializerUtils;
import com.pzx.raft.kv.KVPrefixAdapter;
import com.pzx.raft.kv.PerKVStore;
import com.pzx.raft.kv.entity.Region;
import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.StoreInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

public class KVMetaStorage implements MetaStorage {

    private static final String STORE_PREFIX = "store_";

    private static final String REGION_PREFIX = "region_";

    private final String REGION_ID = "regionId";

    private KVPrefixAdapter storeKVPrefixAdapter;

    private KVPrefixAdapter regionKVPrefixAdapter;

    private PerKVStore kvStore;

    private Map<Long, StoreInfo> storeInfoMap = new ConcurrentHashMap<>();

    private Map<Long, RegionInfo> regionInfoMap = new ConcurrentHashMap<>();

    private long regionId = 0;

    public KVMetaStorage(PerKVStore kvStore) {
        this.kvStore = kvStore;
        this.storeKVPrefixAdapter = new KVPrefixAdapter(kvStore, STORE_PREFIX);
        this.regionKVPrefixAdapter = new KVPrefixAdapter(kvStore, REGION_PREFIX);
        byte[] regionIdBytes = kvStore.get(ByteUtils.stringToBytes(REGION_ID));
        this.regionId = regionIdBytes == null ? 0 : ByteUtils.bytesToLong(regionIdBytes);
    }

    @Override
    public StoreInfo getStoreInfo(long storeId) {
        if(!storeInfoMap.containsKey(storeId)){
            byte[] storeInfoBytes = storeKVPrefixAdapter.get(ByteUtils.longToBytes(storeId));
            if (storeInfoBytes != null)
                storeInfoMap.put(storeId, (StoreInfo) ProtobufSerializerUtils.deserialize(storeInfoBytes, StoreInfo.class));
        }
        return storeInfoMap.get(storeId);
    }

    @Override
    public boolean updateStoreInfo(StoreInfo storeInfo) {
        this.storeInfoMap.put(storeInfo.getStoreId(), storeInfo);
        storeKVPrefixAdapter.put(ByteUtils.longToBytes(storeInfo.getStoreId()), ProtobufSerializerUtils.serialize(storeInfo));
        return true;
    }

    @Override
    public RegionInfo getRegionInfo(long regionId) {
        if(!regionInfoMap.containsKey(regionId)){
            byte[] regionInfoBytes = regionKVPrefixAdapter.get(ByteUtils.longToBytes(regionId));
            if (regionInfoBytes != null)
                regionInfoMap.put(regionId, (RegionInfo) ProtobufSerializerUtils.deserialize(regionInfoBytes, RegionInfo.class));
        }
        return regionInfoMap.get(regionId);
    }

    @Override
    public boolean updateRegionInfo(RegionInfo regionInfo) {
        this.regionInfoMap.put(regionInfo.getRegion().getRegionId(), regionInfo);
        regionKVPrefixAdapter.put(ByteUtils.longToBytes(regionInfo.getRegion().getRegionId()), ProtobufSerializerUtils.serialize(regionInfo));
        return true;
    }

    @Override
    public long createRegionId() {
        synchronized (REGION_ID){
            regionId += 1;
            kvStore.put(ByteUtils.stringToBytes(REGION_ID), ByteUtils.longToBytes(regionId));
        }
        return regionId;
    }

    @Override
    public long getMaxRegionId() {
        return regionId;
    }

    @Override
    public Lock getWriteLock() {
        return kvStore.getWriteLock();
    }

    @Override
    public Lock getReadLock() {
        return kvStore.getReadLock();
    }
}
