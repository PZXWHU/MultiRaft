package com.pzx.raft.kv.meta;

import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.StoreInfo;

import java.util.concurrent.locks.Lock;

public interface MetaStorage {

    StoreInfo getStoreInfo(long storeId);

    boolean updateStoreInfo(StoreInfo storeInfo);

    RegionInfo getRegionInfo(long regionId);

    boolean updateRegionInfo(RegionInfo regionInfo);

    long createRegionId();

    long getMaxRegionId();

    Lock getWriteLock();

    Lock getReadLock();

}
