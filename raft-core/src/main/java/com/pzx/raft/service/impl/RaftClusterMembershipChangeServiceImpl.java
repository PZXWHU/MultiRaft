package com.pzx.raft.service.impl;

import com.pzx.raft.config.NodeConfig;
import com.pzx.raft.config.RaftConfig;
import com.pzx.raft.log.Command;
import com.pzx.raft.log.LogEntry;
import com.pzx.raft.node.RaftNode;
import com.pzx.raft.service.RaftClusterMembershipChangeService;
import com.pzx.raft.service.entity.ClusterMembershipChangeRequest;
import com.pzx.raft.service.entity.ClusterMembershipChangeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * https://www.cnblogs.com/foxmailed/p/7190642.html
 * https://segmentfault.com/a/1190000022796386
 * http://oserror.com/distributed/raft-principle-two/
 * 这里是模仿etcd中的实现方式：
 * 1. 每次只增加或者减少一个节点，所以不会存在raft论文中存在的更换配置时可能会出现两个leader
 * 2. 成员增减生效的时机是在apply成员变更日志时，而不是raft论文中成员变更日志写盘的时候，不管是否commit。
 * 因为raft论文的实现方式会导致如果这条成员变更日志最终没有commit，在发生leader切换的时候，成员组就需要回滚到旧的成员组。
 */
public class RaftClusterMembershipChangeServiceImpl implements RaftClusterMembershipChangeService {

    private static final Logger logger = LoggerFactory.getLogger(RaftClusterMembershipChangeServiceImpl.class);
    private RaftNode raftNode;

    public RaftClusterMembershipChangeServiceImpl(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    /**
     * 将成员变更日志复制到大多数节点
     * @param request
     * @return
     */
    @Override
    public ClusterMembershipChangeResponse addNode(ClusterMembershipChangeRequest request) {
        ClusterMembershipChangeResponse response = ClusterMembershipChangeResponse.builder().success(false).build();
        if (raftNode.getLeaderId() == 0){
            response.setMessage("集群不可用：还未选举出leader节点！");
            return response;
        }else if (raftNode.getLeaderId() != NodeConfig.nodeId){
            //redirect to leader
            return (ClusterMembershipChangeResponse) raftNode.getPeerMap().get(raftNode.getLeaderId())
                    .getRaftClusterMembershipChangeServiceSync().addNode(request);
        }else {
            if (raftNode.getPeerMap().containsKey(request.getNodeId())){
                logger.warn("the node is already in cluster");
                response.setMessage("the node is already in cluster");
                return response;
            }
            Command addNodeCommand = new Command();
            Map<Integer, String> newClusterAddress = new HashMap<>(raftNode.getRaftConfig().getClusterAddress());
            newClusterAddress.put(request.getNodeId(), request.getNodeAddress());
            addNodeCommand.setKey(RaftConfig.CLUSTER_ADDRESS_FIELD_NAME);
            addNodeCommand.setValue(newClusterAddress);
            addNodeCommand.setCommandType(Command.CommandType.CONFIGURATION);
            LogEntry logEntry = new LogEntry(-1, raftNode.getCurrentTerm(), addNodeCommand);

            if (raftNode.replicateEntry(logEntry)){
                response.setSuccess(true);
                response.setMessage("添加节点成功 ： nodeID :" + request.getNodeId() + " " + request.getNodeAddress());
            }else {
                response.setMessage("添加节点失败 ： nodeID :" + request.getNodeId() + " " + request.getNodeAddress());
            }
            return response;
        }

    }

    @Override
    public ClusterMembershipChangeResponse removeNode(ClusterMembershipChangeRequest request) {
        ClusterMembershipChangeResponse response = ClusterMembershipChangeResponse.builder().success(false).build();
        if (raftNode.getLeaderId() == 0){
            response.setMessage("集群不可用：还未选举出leader节点！");
            return response;
        }else if (raftNode.getLeaderId() != NodeConfig.nodeId){
            //redirect to leader
            return (ClusterMembershipChangeResponse) raftNode.getPeerMap().get(raftNode.getLeaderId())
                    .getRaftClusterMembershipChangeServiceSync().removeNode(request);
        }else {
            if (!raftNode.getPeerMap().containsKey(request.getNodeId())){
                logger.warn("the node is not in cluster");
                response.setMessage("the node is not in cluster");
                return response;
            }
            Command removeNodeCommand = new Command();
            Map<Integer, String> newClusterAddress = new HashMap<>(raftNode.getRaftConfig().getClusterAddress());
            newClusterAddress.remove(request.getNodeId());
            removeNodeCommand.setKey(RaftConfig.CLUSTER_ADDRESS_FIELD_NAME);
            removeNodeCommand.setValue(newClusterAddress);
            removeNodeCommand.setCommandType(Command.CommandType.CONFIGURATION);
            LogEntry logEntry = new LogEntry(-1, raftNode.getCurrentTerm(), removeNodeCommand);

            if (raftNode.replicateEntry(logEntry)){
                response.setSuccess(true);
                response.setMessage("删除节点成功 ： nodeID :" + request.getNodeId() + " " + request.getNodeAddress());
            }else {
                response.setMessage("删除节点失败 ： nodeID :" + request.getNodeId() + " " + request.getNodeAddress());
            }
            return response;
        }

    }
}
