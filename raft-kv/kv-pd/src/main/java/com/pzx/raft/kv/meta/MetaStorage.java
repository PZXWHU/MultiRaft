package com.pzx.raft.kv.meta;

public interface MetaStorage {

    StoreMeta getStoreMeta(long StoreId);

    boolean updateStoreMeta(StoreMeta storeMeta);

    RegionMeta getRegionReta(long regionId);

    boolean updateRegionMeta(RegionMeta regionMeta);

    long createRegionId();

}
