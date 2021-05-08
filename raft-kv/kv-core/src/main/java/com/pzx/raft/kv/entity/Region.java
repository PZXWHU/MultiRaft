package com.pzx.raft.kv.entity;

import com.pzx.raft.core.utils.ByteUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter

public class Region {

    private long              regionId;                                             // region id
    // Region key range [startKey, endKey)
    private byte[]            startKey;                                       // inclusive
    private byte[]            endKey;                                         // exclusive

    private long regionEpoch;

    //raft复制组的地址
    private Map<Long, String> raftGroupAddress;//

    public Region() {
    }

    public Region(long regionId, byte[] startKey, byte[] endKey, long regionEpoch, Map<Long, String> raftGroupAddress) {
        this.regionId = regionId;
        this.startKey = startKey;
        this.endKey = endKey;
        this.regionEpoch = regionEpoch;
        this.raftGroupAddress = raftGroupAddress;
    }

    public Region copy(){
        Region region = new Region();
        region.setRegionId(regionId);
        region.setStartKey(startKey);
        region.setEndKey(endKey);
        region.setRegionEpoch(regionEpoch);
        region.setRaftGroupAddress(new HashMap<>(raftGroupAddress));
        return region;
    }

    @Override
    public String toString() {
        return "Region{" +
                "regionId=" + regionId +
                ", startKey=" + ByteUtils.bytesToString(startKey) +
                ", endKey=" + ByteUtils.bytesToString(endKey) +
                ", regionEpoch=" + regionEpoch +
                ", raftGroupAddress='" + raftGroupAddress + '\'' +
                '}';
    }
}
