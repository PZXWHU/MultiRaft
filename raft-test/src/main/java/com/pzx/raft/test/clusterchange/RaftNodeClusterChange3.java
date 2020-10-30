package com.pzx.raft.test.clusterchange;

import com.pzx.raft.node.RaftNode;
import com.pzx.raft.service.RaftClusterMembershipChangeService;
import com.pzx.raft.service.entity.ClusterMembershipChangeRequest;
import com.pzx.raft.service.entity.ClusterMembershipChangeResponse;
import com.pzx.raft.test.election.RaftNodeElection1;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.service.proxy.ProxyConfig;

public class RaftNodeClusterChange3 {

    public static void main(String[] args) {
        ProxyConfig proxyConfig = new ProxyConfig().setDirectServerUrl("127.0.0.1:9999").setInvokeType(InvokeType.SYNC);
        RaftClusterMembershipChangeService raftClusterMembershipChangeService = proxyConfig.getProxy(RaftClusterMembershipChangeService.class);
        ClusterMembershipChangeRequest request = ClusterMembershipChangeRequest.builder().nodeId(1).nodeAddress("127.0.0.1:9999").build();
        ClusterMembershipChangeResponse response = raftClusterMembershipChangeService.removeNode(request);
        System.out.println(response);
    }

}
