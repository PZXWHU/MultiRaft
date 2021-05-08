package com.pzx.raft.core.service.impl;

import com.pzx.raft.core.entity.KVOperation;
import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.core.node.RaftNode;
import com.pzx.raft.core.service.RaftKVClientService;
import com.pzx.raft.core.entity.LogEntry;
import com.pzx.raft.core.service.entity.KVClientRequest;
import com.pzx.raft.core.service.entity.KVClientResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public class RaftKVClientServiceImpl implements RaftKVClientService {

    private RaftNode raftNode;

    public RaftKVClientServiceImpl(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public KVClientResponse operateKV(KVClientRequest request) {
        if (raftNode.getLeaderId() == 0){
            return KVClientResponse.builder().success(false).message("集群不可用：还未选举出leader节点！").build();
        }else if (raftNode.getLeaderId() != raftNode.getServerId()){
            //redirect to leader
            return (KVClientResponse) raftNode.getPeers().get(raftNode.getLeaderId()).getRaftKVClientServiceSync().operateKV(request);
        }else {
            if (request.getKvOperation() == KVOperation.GET){
                return KVClientResponse.builder().success(true).message("获取数据成功").result(raftNode.getRaftStateMachine().get(request.getKey())).build();
            }else {
                SMCommand command = new SMCommand(request.getKey(), request.getValue());
                boolean success = false;
                try {
                    success = raftNode.replicateEntry(command).get();
                }catch (InterruptedException | ExecutionException e){
                    log.warn(e.getMessage());
                }
                if (success)
                    return KVClientResponse.builder().success(true).message("设置数据成功").build();
                else
                    return KVClientResponse.builder().success(false).message("设置数据失败").build();
            }
        }

    }
}
