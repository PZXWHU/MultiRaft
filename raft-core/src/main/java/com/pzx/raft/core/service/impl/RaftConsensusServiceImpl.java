package com.pzx.raft.core.service.impl;

import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.core.entity.ConfigCommand;
import com.pzx.raft.core.entity.SMCommand;
import com.pzx.raft.core.node.RaftNode;
import com.pzx.raft.core.service.RaftConsensusService;
import com.pzx.raft.core.service.entity.*;
import com.pzx.raft.core.utils.MyFileUtils;
import com.pzx.raft.core.entity.LogEntry;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 本服务只在集群内部调用，所以如果接收到的Rpc不来自集群内部节点，则直接返回默认值。
 */
public class RaftConsensusServiceImpl implements RaftConsensusService {

    private static final Logger logger = LoggerFactory.getLogger(RaftConsensusServiceImpl.class);

    private RaftNode raftNode;

    public RaftConsensusServiceImpl(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    /**
     *
     * 1. 如果当前有确认的leader，则忽略请求投票的RPC
     * 2. 如果当前任期大于请求投票中的任期，则拒绝为其投票
     * 3. 如果当前任期小于请求投票中的人去，则处理任期落后
     * 4. 如果请求投票中的日志信息比当前节点的日志更新，则同意投票。否则拒绝投票
     * @param request
     * @return
     */
    @Override
    public ReqVoteResponse requestVote(ReqVoteRequest request) {
        ReqVoteResponse reqVoteResponse = ReqVoteResponse.builder().currentTerm(raftNode.getCurrentTerm()).voteGranted(false).build();

        if (!raftNode.getPeers().containsKey(request.getCandidateId()))
            return reqVoteResponse;
        if (raftNode.getLeaderId() != 0)
            return reqVoteResponse;

        raftNode.getWriteLock().lock();
        try {
            if (request.getCandidateTerm() < raftNode.getCurrentTerm() )
                return reqVoteResponse;
            else if (request.getCandidateTerm() > raftNode.getCurrentTerm())
                raftNode.handleTermLag(request.getCandidateTerm());

            boolean logIsOk = request.getLastLogTerm() > raftNode.getLastLogTerm()
                    || (request.getLastLogTerm() == raftNode.getLastLogTerm()
                    && request.getLastLogIndex() >= raftNode.getRaftLogStorage().getLastIndex());
            if (raftNode.getVotedFor() == 0 && logIsOk) {
                raftNode.resetElectionTimer();
                raftNode.setVotedFor(request.getCandidateId());
                raftNode.getRaftMetaStorage().setVotedFor(request.getCandidateId());
                reqVoteResponse.setVoteGranted(true);
            }
            return reqVoteResponse;
        }finally {
            raftNode.getWriteLock().unlock();
        }

    }

    /**
     * 1. 返回假 如果领导者的任期 小于 接收者的当前任期（译者注：这里的接收者是指跟随者或者候选者）（5.1 节）
     * 2. 返回假 如果接收者日志中没有包含这样一个条目 即该条目的任期在preLogIndex上能和prevLogTerm匹配上 （译者注：在接收者日志中 如果能找到一个和preLogIndex以及prevLogTerm一样的索引和任期的日志条目 则返回真 否则返回假）（5.3 节）
     * 3. 如果一个已经存在的条目和新条目（译者注：即刚刚接收到的日志条目）发生了冲突（因为索引相同，任期不同），那么就删除这个已经存在的条目以及它之后的所有条目 （5.3 节）
     * 4. 追加日志中尚未存在的任何新条目
     * 5. 如果领导者的已知已经提交的最高的日志条目的索引 大于 接收者的已知已经提交的最高的日志条目的索引 则把 接收者的已知已经提交的最高的日志条目的索引 重置为 领导者的已知已经提交的最高的日志条目的索引 或者是 上一个新条目的索引 取两者的最小值
     * @param request
     * @return
     */
    @Override
    public AppendEntriesResponse appendEntries(AppendEntriesRequest request) {
        AppendEntriesResponse appendEntriesResponse = AppendEntriesResponse.builder().currentTerm(raftNode.getCurrentTerm()).success(false).build();

        if (!raftNode.getPeers().containsKey(request.getLeaderId())){
            return appendEntriesResponse;
        }

        raftNode.getWriteLock().lock();
        try {

            //1. 如果leader的任期小于当前节点任期，则返回false。如果leader的任期大于当前节点任期，则处理任期落后
            if (request.getLeaderTerm() < raftNode.getCurrentTerm())
                return appendEntriesResponse;
            else if (request.getLeaderTerm()> raftNode.getCurrentTerm())
                raftNode.handleTermLag(request.getLeaderTerm());

            //2. 如果还没有设置leader，则确定leader，不可能出现raftNode.getLeaderId() != 0 &&  raftNode.getLeaderId() != request.getLeaderId()的情况
            if (raftNode.getLeaderId() == 0){
                raftNode.setLeaderId(request.getLeaderId());
                raftNode.setState(RaftNode.NodeState.FOLLOWER);
                logger.info("new leaderId={}, conf={}", raftNode.getLeaderId(), raftNode.getRaftConfig().toString());
            }

            //3. 重置选举计时器，避免选举超时导致进行选举
            raftNode.resetElectionTimer();

            //4. 判断PrevLogIndex、PrevLogTerm是否能够对应上
            if (request.getPrevLogIndex() > raftNode.getRaftLogStorage().getLastIndex()) {
                logger.info("Rejecting AppendEntries RPC would leave gap, request prevLogIndex={}, my lastLogIndex={}",
                        request.getPrevLogIndex(), raftNode.getRaftLogStorage().getLastIndex());
                return appendEntriesResponse;
            }
            raftNode.getRaftLogStorage().getWriteLock().lock();
            long raftLogFirstIndex;
            try {
                raftLogFirstIndex = raftNode.getRaftLogStorage().getLastIndex() - raftNode.getRaftLogStorage().getTotalSize() + 1;
            }finally {
                raftNode.getRaftLogStorage().getWriteLock().unlock();
            }
            if (request.getPrevLogIndex() >= raftLogFirstIndex
                    && raftNode.getRaftLogStorage().read(request.getPrevLogIndex()).getTerm() != request.getPrevLogTerm()) {
                logger.info("Rejecting AppendEntries RPC: terms don't agree, request prevLogTerm={} in prevLogIndex={}, my is {}",
                        request.getPrevLogTerm(), request.getPrevLogIndex(),
                        raftNode.getRaftLogStorage().read(request.getPrevLogIndex()).getTerm());
                return appendEntriesResponse;
            }

            appendEntriesResponse.setSuccess(true);
            //5. PrevLogIndex、PrevLogTerm能够对应上，则判断是否是心跳
            if (request.getEntries() == null || request.getEntries().size() == 0) {
                logger.debug("heartbeat request from peer={} at term={}, my term={}",
                        request.getLeaderId(), request.getLeaderTerm(), raftNode.getCurrentTerm());
                advanceCommitIndex(request);
                return appendEntriesResponse;
            }

            //6. PrevLogIndex、PrevLogTerm是能够对应上，则将request中的日志复制到节点日志中
            long index = request.getPrevLogIndex();
            for(LogEntry logEntry : request.getEntries()){
                index++;
                if (index < raftLogFirstIndex) continue;//说明以及生成了快照，旧日志已经被删除，
                if (raftNode.getRaftLogStorage().getLastIndex() >= index){
                    //index、term相同，则日志条目一定是相同的
                    if (raftNode.getRaftLogStorage().read(index).getTerm() == logEntry.getTerm()) {
                        continue;
                    }
                    raftNode.getRaftLogStorage().removeFromStartIndex(index);
                }
                raftNode.getRaftLogStorage().write(logEntry);
            }
            advanceCommitIndex(request);
            logger.info("AppendEntries request from server {} in term {} (my term is {}), entryCount={} ",
                    request.getLeaderId(), request.getLeaderTerm(), raftNode.getCurrentTerm(), request.getEntries().size());
            return appendEntriesResponse;

        }finally {
            raftNode.getWriteLock().unlock();
        }
    }

    /**
     * 1. 求得新的CommitIndex
     * 2. 更新Node的状态
     * 3. 将新提交的日志应用到状态集中
     * @param request
     */
    private void advanceCommitIndex(AppendEntriesRequest request){
        long newCommitIndex = Math.min(request.getLeaderCommit(), request.getPrevLogIndex() + request.getEntriesNum());
        raftNode.setCommitIndex(newCommitIndex);

        if (raftNode.getLastAppliedIndex() < raftNode.getCommitIndex()) {
            // apply state machine
            for (long index = raftNode.getLastAppliedIndex() + 1; index <= raftNode.getCommitIndex(); index++) {
                LogEntry logEntry = raftNode.getRaftLogStorage().read(index);
                if (logEntry.getCommand() instanceof SMCommand)
                    raftNode.getRaftStateMachine().apply(logEntry);
                else if (logEntry.getCommand() instanceof ConfigCommand)
                    raftNode.applyConfiguration(logEntry);
                raftNode.setLastAppliedIndex(index);
                raftNode.getRaftMetaStorage().setLastAppliedIndex(index);
            }
        }
    }


    /**
     * 1. 如果term < currentTerm就立即回复
     * 2. 如果是第一个分块（offset 为 0）就创建一个新的快照
     * 3. 在指定偏移量写入数据
     * 4. 如果 done 是 false，则继续等待更多的数据
     * 5. 保存快照文件，丢弃具有较小索引的任何现有或部分快照
     * 6. 如果现存的日志条目与快照中最后包含的日志条目具有相同的索引值和任期号，则保留其后的日志条目并进行回复
     * 7. 丢弃整个日志
     * 8. 使用快照重置状态机（并加载快照的集群配置）
     * @param request
     * @return
     */
    @Override
    public InstallSnapshotResponse installSnapshot(InstallSnapshotRequest request) {
        InstallSnapshotResponse installSnapshotResponse = InstallSnapshotResponse.builder().currentTerm(raftNode.getCurrentTerm()).success(false).build();

        if (!raftNode.getPeers().containsKey(request.getLeaderId())){

            return installSnapshotResponse;
        }

        //1. 一些取小install snapshot的情况
        if (raftNode.getIsTakeSnapshot().get()){
            logger.warn("already in take snapshot, do not handle install snapshot request now");
            return installSnapshotResponse;
        }


        raftNode.getWriteLock().lock();
        try {
            //2. 处理term不正常的情况
            if (request.getLeaderTerm() < raftNode.getCurrentTerm())
                return installSnapshotResponse;
            else if (request.getLeaderTerm() > raftNode.getCurrentTerm())
                raftNode.handleTermLag(request.getLeaderTerm());

            //3. 重置选举计时器，避免选举超时导致进行选举。（install snapshot时，并不会发送心跳）
            raftNode.resetElectionTimer();

            //4. 将收到的数据写入临时快照文件夹中
            String tmpSnapshotDir = raftNode.getNodeConfig().getSnapshotDir() + ".tmp";
            File tmpSnapshotDirFile = new File(tmpSnapshotDir);
            if (request.getOffset() == 0){
                if (!raftNode.getIsInstallSnapshot().compareAndSet(false, true)){
                    logger.warn("already in install snapshot, do not handle install snapshot request now");
                    return installSnapshotResponse;
                }
                FileUtils.deleteDirectory(tmpSnapshotDirFile);
                FileUtils.forceMkdir(tmpSnapshotDirFile);
            }

            Path snapshotFile = Paths.get(tmpSnapshotDir, request.getSnapshotFileName());
            snapshotFile.toFile().getAbsoluteFile().getParentFile().mkdirs();
            Files.write(snapshotFile, request.getData(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            installSnapshotResponse.setSuccess(true);
            logger.info("install snapshot request from node {} in term {} (my term is {}) successful, offset : {}",
                    request.getLeaderId(), request.getLeaderTerm(), raftNode.getCurrentTerm(), request.getOffset());

            //5. 如果收到的install snapshot请求是最后一个：
            if (request.isDone()){
                //5.1 将临时快照文件夹替代之前的快照文件夹
                File snapshotDirFile = new File(raftNode.getNodeConfig().getSnapshotDir());
                FileUtils.deleteDirectory(snapshotDirFile);
                FileUtils.moveDirectory(tmpSnapshotDirFile, snapshotDirFile);

                //5.2 节点读取快照（加载快照元数据，状态机读取快照，加载节点配置以及状态变量commitIndex、currentTerm、lastAppliedIndex）
                raftNode.getRaftMetaStorage().setSnapshotLastIncludedIndex(request.getLastIncludedIndex());
                raftNode.getRaftMetaStorage().setSnapshotLastIncludedTerm(request.getLastIncludedTerm());
                raftNode.loadSnapshot();

                //5.3 丢弃包含在快照中的旧日志
                LogEntry emptyEntry = LogEntry.builder().command(new SMCommand()).build();
                while (raftNode.getRaftLogStorage().getLastIndex() < raftNode.getRaftMetaStorage().getSnapshotLastIncludedIndex())
                    raftNode.getRaftLogStorage().write(emptyEntry);//为了将raftLog的lastIndex更新到快照中LastIncludedIndex
                raftNode.getRaftLogStorage().removeToEndIndex(raftNode.getRaftMetaStorage().getSnapshotLastIncludedIndex());

                logger.info("end accept install snapshot request from serverId={}", request.getLeaderId());
            }

        }catch (Exception e){
            e.printStackTrace();
            logger.warn("when handle installSnapshot request, meet exception:" + e);
            raftNode.getIsInstallSnapshot().compareAndSet(true, false);
        }finally {
            raftNode.getWriteLock().lock();
            if (request.isDone())
                raftNode.getIsInstallSnapshot().compareAndSet(true, false);
        }
        return installSnapshotResponse;


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
        }else if (raftNode.getLeaderId() != raftNode.getServerId()){
            //redirect to leader
            return (ClusterMembershipChangeResponse) raftNode.getPeers().get(raftNode.getLeaderId())
                    .getRaftConsensusServiceSync().addNode(request);
        }else {
            if (raftNode.getPeers().containsKey(request.getServerId()) || raftNode.getServerId() == request.getServerId()){
                logger.warn("the node is already in cluster");
                response.setMessage("the node is already in cluster");
                return response;
            }
            ConfigCommand addNodeCommand = new ConfigCommand();
            Map<Long, String> newRaftGroupAddress = RaftConfig.parseRaftGroupAddress(raftNode.getRaftConfig().getRaftGroupAddress());
            newRaftGroupAddress.put(request.getServerId(), request.getServerAddress());
            addNodeCommand.setKey(RaftConfig.RAFT_GROUP_ADDRESS_Field);
            addNodeCommand.setValue(RaftConfig.unParseRaftGroupAddress(newRaftGroupAddress));
            LogEntry logEntry = new LogEntry(-1, raftNode.getCurrentTerm(), addNodeCommand);
            System.out.println(logEntry);
            boolean success = false;
            try {
                success = raftNode.replicateEntry(logEntry).get();
                response.setMessage("添加节点成功 ： ServerId :" + request.getServerId() + " " + request.getServerAddress());
            }catch (InterruptedException | ExecutionException e){
                logger.warn(e.getMessage());
                response.setMessage("添加节点失败 ： ServerId :" + request.getServerId() + " " + request.getServerAddress());
            }
            response.setSuccess(success);
            return response;
        }

    }

    @Override
    public ClusterMembershipChangeResponse removeNode(ClusterMembershipChangeRequest request) {
        ClusterMembershipChangeResponse response = ClusterMembershipChangeResponse.builder().success(false).build();
        if (raftNode.getLeaderId() == 0){
            response.setMessage("集群不可用：还未选举出leader节点！");
            return response;
        }else if (raftNode.getLeaderId() != raftNode.getServerId()){
            //redirect to leader
            return (ClusterMembershipChangeResponse) raftNode.getPeers().get(raftNode.getLeaderId())
                    .getRaftConsensusServiceSync().removeNode(request);
        }else {
            if (!raftNode.getPeers().containsKey(request.getServerId()) && raftNode.getServerId() != request.getServerId()){
                logger.warn("the node is not in cluster");
                response.setMessage("the node is not in cluster");
                return response;
            }
            ConfigCommand removeNodeCommand = new ConfigCommand();
            Map<Long, String> newRaftGroupAddress = RaftConfig.parseRaftGroupAddress(raftNode.getRaftConfig().getRaftGroupAddress());
            if (!newRaftGroupAddress.remove(request.getServerId()).equals(request.getServerAddress())){
                response.setMessage("the node address is not correct");
                return response;
            }
            removeNodeCommand.setKey(RaftConfig.RAFT_GROUP_ADDRESS_Field);
            removeNodeCommand.setValue(RaftConfig.unParseRaftGroupAddress(newRaftGroupAddress));
            LogEntry logEntry = new LogEntry(-1, raftNode.getCurrentTerm(), removeNodeCommand);

            boolean success = false;
            try {
                success = raftNode.replicateEntry(logEntry).get();
                response.setMessage("删除节点成功 ： ServerId :" + request.getServerId() + " " + request.getServerAddress());
            }catch (InterruptedException | ExecutionException e){
                logger.warn(e.getMessage());
                response.setMessage("删除节点失败 ： ServerId :" + request.getServerId() + " " + request.getServerAddress());
            }
            response.setSuccess(success);
            return response;

        }

    }

}
