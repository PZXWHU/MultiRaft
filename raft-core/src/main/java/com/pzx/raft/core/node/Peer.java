package com.pzx.raft.core.node;

import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.core.service.RaftKVClientService;
import com.pzx.raft.core.service.entity.AppendEntriesRequest;
import com.pzx.raft.core.service.entity.ReqVoteRequest;
import com.pzx.rpc.context.RpcInvokeContext;
import com.pzx.rpc.enumeration.InvokeType;
import com.pzx.rpc.invoke.RpcResponseCallBack;
import com.pzx.rpc.service.proxy.ProxyConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * @author PZX
 */
@Getter
@Setter
public class Peer {

    //节点id
    private long serverId;

    private String peerAddress;

    // 需要发送给该follower的下一个日志条目的索引值
    private long nextIndex = 1;

    // 已复制到该节点上的日志条目的最高索引值
    private long matchIndex = 0;

    private RaftConsensusService raftConsensusServiceAsync;

    private RaftConsensusService raftConsensusServiceSync;

    private RaftKVClientService raftKVClientServiceSync;

    public Peer(long serverId, String peerAddress, long nextIndex) {
        this.serverId = serverId;
        this.peerAddress = peerAddress;
        this.nextIndex = nextIndex;

        ProxyConfig proxyConfig = new ProxyConfig().setInvokeType(InvokeType.CALLBACK).setDirectServerUrl(peerAddress).setTimeout(10000);
        raftConsensusServiceAsync = proxyConfig.getProxy(RaftConsensusService.class);
        proxyConfig.setInvokeType(InvokeType.SYNC);
        raftConsensusServiceSync = proxyConfig.getProxy(RaftConsensusService.class);
        raftKVClientServiceSync = proxyConfig.getProxy(RaftKVClientService.class);

    }

    public void requestVote(ReqVoteRequest reqVoteRequest, RpcResponseCallBack callBack){
        RpcInvokeContext.getContext().setResponseCallback(callBack);
        raftConsensusServiceAsync.requestVote(reqVoteRequest);
    }

    public void appendEntries(AppendEntriesRequest appendEntriesRequest, RpcResponseCallBack callBack){
        RpcInvokeContext.getContext().setResponseCallback(callBack);
        raftConsensusServiceAsync.appendEntries(appendEntriesRequest);
    }

}
