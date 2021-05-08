package com.pzx.raft.test.core.clusterchange;

import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.core.service.entity.ClusterMembershipChangeRequest;
import com.pzx.raft.core.service.entity.ClusterMembershipChangeResponse;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.service.proxy.ProxyConfig;

public class RaftNodeClusterChange3 {

    public static void main(String[] args) {
        ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:8003").setInvokeType(InvokeType.SYNC);
        RaftConsensusService raftConsensusService = proxyConfig.getProxy(RaftConsensusService.class);
        ClusterMembershipChangeRequest request = ClusterMembershipChangeRequest.builder().serverId(2).serverAddress("127.0.0.1:8002").build();
        ClusterMembershipChangeResponse response = raftConsensusService.removeNode(request);
        System.out.println(response);
    }

}
