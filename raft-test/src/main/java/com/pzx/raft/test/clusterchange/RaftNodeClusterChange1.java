package com.pzx.raft.test.clusterchange;

import com.pzx.raft.service.RaftClusterMembershipChangeService;
import com.pzx.raft.service.entity.ClusterMembershipChangeRequest;
import com.pzx.raft.service.entity.ClusterMembershipChangeResponse;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.service.proxy.ProxyConfig;

public class RaftNodeClusterChange1 {
    public static void main(String[] args) {
        ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:9999").setInvokeType(InvokeType.SYNC);
        RaftClusterMembershipChangeService raftClusterMembershipChangeService = proxyConfig.getProxy(RaftClusterMembershipChangeService.class);
        ClusterMembershipChangeRequest request = ClusterMembershipChangeRequest.builder().nodeId(4).nodeAddress("127.0.0.1:6999").build();
        ClusterMembershipChangeResponse response = raftClusterMembershipChangeService.addNode(request);
        System.out.println(response);
    }

}
