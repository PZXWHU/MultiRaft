package com.pzx.raft.kv;

import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.kv.entity.Region;
import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.meta.MetaStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.StampedLock;

@Slf4j
public class RegionRouteTable {

    private NavigableMap<byte[], Long> rangeTable = new ConcurrentSkipListMap<>(ByteUtils.BYTES_LEXICO_COMPARATOR);

    private MetaStorage metaStorage;

    public RegionRouteTable(MetaStorage metaStorage) {
        this.metaStorage = metaStorage;
        initRangeTable();
    }

    private void initRangeTable(){

        long maxRegionId = metaStorage.getMaxRegionId();
        for(long i = 1; i <= maxRegionId; i++){
            RegionInfo regionInfo = metaStorage.getRegionInfo(i);
            if (regionInfo != null)
                rangeTable.put(regionInfo.getRegion().getStartKey(), regionInfo.getRegion().getRegionId());
        }
        log.info("region set :" + rangeTable.values());
    }

    /**
     * Returns the region to which the key belongs.
     */
    public RegionInfo findRegionByKey(byte[] key) {
        if (key == null) return null;
        final Map.Entry<byte[], Long> entry = this.rangeTable.floorEntry(key);
        /*System.out.println(ByteUtils.compare(key,
                entry.getKey()));
        System.out.println(ByteUtils.bytesToInteger(key) + "  " +
                ByteUtils.bytesToInteger(entry.getKey()));*/
        return metaStorage.getRegionInfo(entry.getValue());
    }



    /**
     * Returns the list of regions covered by startKey and endKey.
     */
    public List<RegionInfo> findRegionsByKeyRange(byte[] startKey, byte[] endKey){

        final NavigableMap<byte[], Long> subRegionMap;
        if (endKey == null) {
            subRegionMap = this.rangeTable.tailMap(startKey, false);
        } else {
            subRegionMap = this.rangeTable.subMap(startKey, false, endKey, true);
        }
        List<RegionInfo> res = new ArrayList<>(subRegionMap.size() + 1);
        for(long regionId : subRegionMap.values())
            res.add(metaStorage.getRegionInfo(regionId));

        Map.Entry<byte[], Long> entry = this.rangeTable.floorEntry(startKey);
        res.add(metaStorage.getRegionInfo(entry.getValue()));

        return res;

    }

    public void addOrUpdateRegion(Region region){
        if (region == null) return;
        rangeTable.put(region.getStartKey(), region.getRegionId());
    }



}
