package com.pzx.raft.service;

import com.pzx.raft.service.entity.ClusterMembershipChangeRequest;
import com.pzx.raft.service.entity.ClusterMembershipChangeResponse;

public interface RaftClusterMembershipChangeService {

    /**
     * 增加一个集群节点
     * @param request
     * @return
     */
    ClusterMembershipChangeResponse addNode(ClusterMembershipChangeRequest request);

    /**
     * 移出一个集群节点
     * @param request
     * @return
     */
    ClusterMembershipChangeResponse removeNode(ClusterMembershipChangeRequest request);

}
