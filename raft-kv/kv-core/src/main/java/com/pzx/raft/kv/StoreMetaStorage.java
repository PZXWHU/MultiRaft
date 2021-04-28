package com.pzx.raft.kv;

import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.core.utils.ProtobufSerializerUtils;
import com.pzx.raft.kv.entity.Region;
import com.pzx.rpc.serde.ProtobufSerDe;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class StoreMetaStorage {

    private static final String STORE_META_PREFIX = "store_meta_";

    private static final String REGIONS = "regions";

    private Set<Region> regions;

    private KVPrefixAdapter kvPrefixAdapter;

    public StoreMetaStorage(PerKVStore perKVStore){
        this.kvPrefixAdapter = new KVPrefixAdapter(perKVStore, STORE_META_PREFIX);
    }


    public Set<Region> getRegions() {
        if (regions == null){
            byte[] regionsBytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(REGIONS));
            if (regionsBytes != null) regions = (Set<Region>) ProtobufSerializerUtils.deserialize(regionsBytes, Set.class);
        }
        return regions;
    }

    public void setRegions(Set<Region> regions) {
        this.regions = regions;
        kvPrefixAdapter.put(ByteUtils.stringToBytes(REGIONS), ProtobufSerializerUtils.serialize(regions));
    }

}
