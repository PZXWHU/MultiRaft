package com.pzx.raft.test.kv.pd;

import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.kv.PdClient;
import com.pzx.raft.kv.config.PdClientConfig;
import com.pzx.raft.kv.entity.Region;
import com.pzx.raft.kv.entity.RegionInfo;
import com.pzx.raft.kv.entity.StoreInfo;
import com.pzx.rpc.transport.netty.client.ChannelPool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PdClientTest {

    public static void main(String[] args) {
        PdClientConfig pdClientConfig = new PdClientConfig();
        pdClientConfig.setServerAddress("127.0.0.1:8888");
        PdClient pdClient = new PdClient(pdClientConfig);
        /*
        Map<Long, String> raftGroupAddress = new HashMap<Long, String>(){
            {
                put(1L, "127.0.0.1:8001");
                put(2L, "127.0.0.1:8002");
                put(3L, "127.0.0.1:8003");
            }
        };
        Region region = new Region();
        region.setStartKey(ByteUtils.BYTE_ARRAY_MIN_VALUE);
        region.setEndKey(ByteUtils.BYTE_ARRAY_MAX_VALUE);
        region.setRegionId(1);
        region.setRegionEpoch(1);
        region.setRaftGroupAddress(raftGroupAddress);

        StoreInfo storeInfo = new StoreInfo();
        storeInfo.setStoreId(3);
        storeInfo.setRegionIds(Collections.singleton(1L));

        pdClient.setStoreInfo(storeInfo);
        System.out.println(pdClient.getStoreInfo(3));*/
        //System.out.println(pdClient.createRegionId());
        //initCluster(pdClient);


        for(int i = 1; i < 15000; i++){
            RegionInfo regionInfo = pdClient.findRegionByKey(ByteUtils.integerToBytes(i));
            int start = regionInfo.getRegion().getStartKey().length != 0 ?
                    ByteUtils.bytesToInteger(regionInfo.getRegion().getStartKey()) : Integer.MIN_VALUE;
            int end = regionInfo.getRegion().getEndKey() != null ?
                    ByteUtils.bytesToInteger(regionInfo.getRegion().getEndKey()) : Integer.MAX_VALUE;
            if (i < start || i >= end){
                System.out.println(i + "  " + start + "  " + end + "  " + regionInfo);
            }
        }
        System.out.println("jieshu ");
        /*RegionInfo regionInfo = pdClient.getRegionInfo(3);
        System.out.println(ByteUtils.bytesToInteger(regionInfo.getRegion().getStartKey()));
        System.out.println(ByteUtils.bytesToInteger(regionInfo.getRegion().getEndKey()));*/

        /*regionInfo = pdClient.getRegionInfo(2);
        System.out.println(ByteUtils.bytesToInteger(regionInfo.getRegion().getStartKey()));
        System.out.println(ByteUtils.bytesToInteger(regionInfo.getRegion().getEndKey()));*/

        //System.out.println(pdClient.findRegionByKey(ByteUtils.integerToBytes(4000)));


    }


    public static void initCluster(PdClient pdClient){
        Map<Long, String> raftGroupAddress = new HashMap<Long, String>(){
            {
                put(1L, "127.0.0.1:8001");
                put(2L, "127.0.0.1:8002");
                put(3L, "127.0.0.1:8003");
            }
        };

        Region region = new Region();
        region.setStartKey(ByteUtils.BYTE_ARRAY_MIN_VALUE);
        region.setEndKey(ByteUtils.BYTE_ARRAY_MAX_VALUE);
        region.setRegionId(pdClient.createRegionId());
        region.setRegionEpoch(1);
        region.setRaftGroupAddress(raftGroupAddress);

        RegionInfo regionInfo = new RegionInfo();
        regionInfo.setRegion(region);
        regionInfo.setLeaderId(1);
        pdClient.setRegionInfo(regionInfo);


        StoreInfo storeInfo = new StoreInfo();
        storeInfo.setRegionIds(Collections.singleton(1L));

        storeInfo.setStoreId(1);
        pdClient.setStoreInfo(storeInfo);

        storeInfo.setStoreId(2);
        pdClient.setStoreInfo(storeInfo);

        storeInfo.setStoreId(3);
        pdClient.setStoreInfo(storeInfo);




    }

}
