package com.pzx.raft.service.impl;

import com.pzx.raft.config.NodeConfig;
import com.pzx.raft.log.Command;
import com.pzx.raft.log.LogEntry;
import com.pzx.raft.node.RaftNode;
import com.pzx.raft.service.RaftKVClientService;
import com.pzx.raft.service.entity.ClientKVRequest;
import com.pzx.raft.service.entity.ClientKVResponse;

public class RaftKVClientServiceImpl implements RaftKVClientService {

    private RaftNode raftNode;

    public RaftKVClientServiceImpl(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public ClientKVResponse operateKV(ClientKVRequest request) {
        if (raftNode.getLeaderId() == 0){
            return ClientKVResponse.builder().success(false).message("集群不可用：还未选举出leader节点！").build();
        }else if (raftNode.getLeaderId() == NodeConfig.nodeId){
            if (request.getType() == ClientKVRequest.GET){
                return ClientKVResponse.builder().success(true).message("获取数据成功").result(raftNode.getStateMachine().get(request.getKey())).build();
            }else {
                Command command = new Command(request.getKey(), request.getValue());
                LogEntry logEntry = LogEntry.builder().term(raftNode.getCurrentTerm()).command(command).build();
                if (raftNode.replicateEntry(logEntry))
                    return ClientKVResponse.builder().success(true).message("设置数据成功").build();
                else
                    return ClientKVResponse.builder().success(false).message("设置数据失败").build();
            }
        }else {
            //redirect to leader
            return (ClientKVResponse) raftNode.getPeerMap().get(raftNode.getLeaderId()).getRaftKVClientServiceSync().operateKV(request);
        }

    }
}
