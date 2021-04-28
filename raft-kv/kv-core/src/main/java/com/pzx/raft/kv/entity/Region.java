package com.pzx.raft.kv.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Region {

    private long              regionId;                                             // region id
    // Region key range [startKey, endKey)
    private byte[]            startKey;                                       // inclusive
    private byte[]            endKey;                                         // exclusive

    private long regionEpoch;

    //raft复制组的地址
    private String raftGroupAddress;

    public Region() {
    }

    public Region(long regionId, byte[] startKey, byte[] endKey, long regionEpoch) {
        this.regionId = regionId;
        this.startKey = startKey;
        this.endKey = endKey;
        this.regionEpoch = regionEpoch;

    }

    @Override
    public String toString() {
        return "Region{" +
                "regionId=" + regionId +
                ", regionEpoch=" + regionEpoch +
                ", raftGroupAddress='" + raftGroupAddress + '\'' +
                '}';
    }
}
