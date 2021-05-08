package com.pzx.raft.core.node;

import com.alibaba.nacos.common.utils.ConcurrentHashSet;
import com.pzx.raft.core.RaftLogStorage;
import com.pzx.raft.core.RaftMetaStorage;
import com.pzx.raft.core.config.NodeConfig;
import com.pzx.raft.core.config.RaftConfig;
import com.pzx.raft.core.RaftStateMachine;
import com.pzx.raft.core.entity.*;
import com.pzx.raft.core.exception.RaftError;
import com.pzx.raft.core.exception.RaftException;
import com.pzx.raft.core.service.entity.*;
import com.pzx.raft.core.utils.MyFileUtils;
import com.pzx.rpc.invoke.RpcResponseCallBack;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * 加锁情况：
 * 1.修改状态机
 * 2.修改配置
 * 3.take snapshot写时复制
 * 4.修改Node内部各种状态变量
 *
 * @author PZX
 */
@Getter
@Setter
public class RaftNode {

    public enum  NodeState {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    private final static Logger logger = LoggerFactory.getLogger(RaftNode.class);

    //Raft节点配置
    private RaftConfig raftConfig;

    //本节点的同伴节点
    private ConcurrentMap<Long, NodePeer> peers = new ConcurrentHashMap<>();

    //Raft日志:每个条目包含了用于状态机的命令、领导者接收到该条目时的任期以及条目在日志中的索引
    private RaftLogStorage raftLogStorage;

    //raft元数据存储
    private RaftMetaStorage raftMetaStorage;

    //状态机
    private RaftStateMachine raftStateMachine;

    //节点的状态，起始为follower
    private NodeState state = NodeState.FOLLOWER;

    //服务器已知最新的任期（在服务器首次启动的时候初始化为0，单调递增）
    private long currentTerm = 0;

    //当前任期内收到选票的候选者id 如果没有投给任何候选者 则为0(要求不能有节点id为0)
    private long votedFor = 0;

    //目前集群中leader节点的id
    private long leaderId = 0;

    //已知已提交的最高的日志条目的索引（初始值为0，单调递增）
    private long commitIndex = 0;

    //已经被应用到状态机的最高的日志条目的索引（初始值为0，单调递增）
    private long  lastAppliedIndex = 0;

    //表示是否正在安装snapshot，leader向follower安装，leader和follower同时处于installSnapshot状态
    private AtomicBoolean isInstallSnapshot = new AtomicBoolean(false);

    //表示节点自己是否在对状态机做snapshot
    private AtomicBoolean isTakeSnapshot = new AtomicBoolean(false);

    //表示自己获得的vote数
    private AtomicInteger voteGrantedNum = new AtomicInteger(0);

    //RaftNode内部锁，当对节点内部状态变量、日志、状态机、配置进行改变时，必须加锁以保证整体状态的一致性
    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    //写锁
    private ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    //读锁
    private ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

    //RaftNode发送心跳、请求投票、安装快照等动作的线程池
    private ExecutorService  executorService ;

    //RaftNode触发心跳、选举的scheduled线程池
    private ScheduledExecutorService scheduledExecutorService;

    //表示选举过程的ScheduledFuture
    private ScheduledFuture electionScheduledFuture;

    //表示heartbeat的ScheduledFuture
    private ScheduledFuture heartbeatScheduledFuture;

    //表示snapshot的scheduledFuture
    private ScheduledFuture snapshotScheduledFuture;

    //复制日志的future
    private ConcurrentHashMap<Long, CompletableFuture<Boolean>> replicateEntryFutureMap = new ConcurrentHashMap<>();

    private ConcurrentHashSet<UserDefinedCommandListener> userDefinedCommandListeners = new ConcurrentHashSet<>();

    //服务器id
    private long serverId;

    //raft group id
    private long groupId;

    private NodeConfig nodeConfig;

    public RaftNode(NodeConfig nodeConfig){

        this.nodeConfig = nodeConfig;
        this.serverId = nodeConfig.getServerId();
        this.groupId = nodeConfig.getGroupId();
        this.raftConfig = nodeConfig.getRaftConfig();
        this.raftStateMachine = nodeConfig.getRaftStateMachine();
        this.raftLogStorage = nodeConfig.getRaftLogStorage();
        this.raftMetaStorage = nodeConfig.getRaftMetaStorage();

        recover();//恢复上一次宕机的状态

        this.executorService = Executors.newFixedThreadPool(raftConfig.getRaftConsensusThreadNum());
        this.scheduledExecutorService = Executors.newScheduledThreadPool(3);
    }

    public void start(){
        //重置选举定时器
        resetElectionTimer();
        //开启快照任务
        if (snapshotScheduledFuture == null)
            snapshotScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                ()->takeSnapshot(),
                raftConfig.getSnapshotPeriodSeconds(),
                raftConfig.getSnapshotPeriodSeconds(),
                TimeUnit.SECONDS);

    }

    public void stop(){
        if (executorService != null)
            executorService.shutdown();
        if (scheduledExecutorService != null)
            scheduledExecutorService.shutdown();
    }

    /**---------------------------------------初始化----------------------------**/

    private void updatePeers(){
        //初始化同伴节点
        for (Long serverId : peers.keySet()){
            if (!raftConfig.getRaftGroupAddress().containsKey(serverId))
                peers.remove(serverId);
        }

        for(Map.Entry<Long, String> entry : raftConfig.getRaftGroupAddress().entrySet()){
            if (entry.getKey() != serverId)
                this.peers.put(entry.getKey(), new NodePeer(entry.getKey(), entry.getValue(), raftLogStorage.getLastIndex() + 1));
        }
    }

    /**---------------------------------------选举-----------------------------------------**/

    /**
     * 获取随机的选举超时时间
     * @return
     */
    private int getRandomElectionTimeoutMs() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int randomElectionTimeout = raftConfig.getElectionTimeoutMilliseconds() + random.nextInt(0, raftConfig.getElectionTimeoutMilliseconds());
        logger.debug("new election time is after {} ms", randomElectionTimeout);
        return randomElectionTimeout;
    }

    /**
     * 重置选举定时器
     */
    public void resetElectionTimer() {
        //取消Scheduled队列中的选举任务
        if (electionScheduledFuture != null && !electionScheduledFuture.isDone()) {
            electionScheduledFuture.cancel(true);
        }
        //获取随机选举超时时间，重新schedule选举任务
        electionScheduledFuture = scheduledExecutorService.schedule(()->{startNewElection();},
                getRandomElectionTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * 写锁加锁
     *
     * 开始选举：
     *  1.转变成候选人状态
     *  2.自增当前的任期号（currentTerm）
     *  3.给自己投票
     *  4.重置选举超时计时器
     *  5.发送请求投票的 RPC 给其他所有服务器
     */
    private void startNewElection(){

        writeLock.lock();
        try {
            leaderId = 0;//重新选举，消除领导者
            state = NodeState.CANDIDATE;
            currentTerm++;
            logger.info("Running for election in term {}", currentTerm);
            votedFor = this.serverId;
            voteGrantedNum.set(1);//清除之前的投票计数，新的任期目前只有自己投票
            raftMetaStorage.setCurrentTerm(currentTerm);
            raftMetaStorage.setVotedFor(votedFor);
        }finally {
            //重置选举超时计时器
            resetElectionTimer();
            writeLock.unlock();
        }

        for (NodePeer nodePeer : peers.values()){
            executorService.submit(()->requestVote(nodePeer));
        }

    }

    private void requestVote(NodePeer nodePeer){
        ReqVoteRequest reqVoteRequest = ReqVoteRequest.builder()
                .groupId(groupId)
                .candidateId(this.serverId)
                .candidateTerm(currentTerm)
                .lastLogIndex(raftLogStorage.getLastIndex())
                .lastLogTerm(getLastLogTerm())
                .build();
        nodePeer.requestVote(reqVoteRequest, new ReqVoteResponseCallback(nodePeer, reqVoteRequest));
    }

    private class ReqVoteResponseCallback implements RpcResponseCallBack{

        private final NodePeer nodePeer;
        private final ReqVoteRequest reqVoteRequest;

        public ReqVoteResponseCallback(NodePeer nodePeer, ReqVoteRequest reqVoteRequest) {
            this.nodePeer = nodePeer;
            this.reqVoteRequest = reqVoteRequest;
        }

        /**
         * 1. 如果目前的任期不等于reqVoteRequest的任期，则说明reqVoteResponse是之后任期内选举的结果，则忽略
         * 2. 如果reqVoteResponse的任期大于目前的任期，则说明本节点的任期落后了，则进行处理
         * 3. 如果reqVoteResponse表示同意投票，则更新目前获得的票数，如果超过半数，则本节点成为leader
         * @param data
         */
        @Override
        public void onResponse(Object data) {
            ReqVoteResponse reqVoteResponse = (ReqVoteResponse)data;
            writeLock.lock();
            try {
                if (currentTerm != reqVoteRequest.getCandidateTerm() || state != NodeState.CANDIDATE) {
                    logger.info("ignore requestVote RPC result");
                    return;
                }
                if (reqVoteResponse.getCurrentTerm() > currentTerm){
                    logger.info("Received RequestVote response from server {} in term {} (this server's term was {})",
                            nodePeer.getServerId(), reqVoteResponse.getCurrentTerm(), currentTerm);
                    handleTermLag(reqVoteResponse.getCurrentTerm());
                }else {
                    if (reqVoteResponse.isVoteGranted()){
                        int currentVoteGrantedNum  = voteGrantedNum.incrementAndGet();
                        logger.info("Got vote from server {} for term {}, currentVoteGrantedNum={}", nodePeer.getServerId(), currentTerm, currentVoteGrantedNum);
                        if (currentVoteGrantedNum > raftConfig.getRaftGroupSize() / 2){
                            logger.info("Got majority vote, serverId={} become leader", RaftNode.this.serverId);
                            becomeLeader();
                        }
                    }
                }
            }finally {
                writeLock.unlock();
            }

        }
        @Override
        public void onException(Throwable throwable) {
            logger.error("requestVote with nodePeer[{}] failed : {}", nodePeer.getPeerAddress(), throwable.getMessage());
        }
    }

    private void becomeLeader(){
        writeLock.lock();
        try {
            state = NodeState.LEADER;
            leaderId = this.serverId;
            //当一个领导人刚获得权力的时候，他初始化所有的 nextIndex 值为自己的最后一条日志的 index 加 1
            for(NodePeer nodePeer : peers.values()){
                nodePeer.setNextIndex(raftLogStorage.getLastIndex() + 1);
            }
        }finally {
            writeLock.unlock();
        }

        // stop vote timer, leader不需要选举定时器
        if (electionScheduledFuture != null && !electionScheduledFuture.isDone()) {
            electionScheduledFuture.cancel(true);
        }
        // 立即发送心跳
        startNewHeartbeat();
    }

    /**------------------------------------------心跳以及日志复制-------------------------------------**/

    /**
     * 重置心跳定时器
     */
    private void resetHeartbeatTimer() {
        //取消Scheduled队列中的心跳任务
        if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
            heartbeatScheduledFuture.cancel(true);
        }
        //重新schedule心跳任务
        heartbeatScheduledFuture = scheduledExecutorService.schedule(()->startNewHeartbeat(),
                raftConfig.getHeartbeatPeriodMilliseconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * 开始发送心跳
     */
    private void startNewHeartbeat() {
        logger.info("start new heartbeat, peers={}", peers.keySet());
        for (NodePeer nodePeer : peers.values()) {
            executorService.submit(()->appendEntries(nodePeer));//发送心跳
        }
        resetHeartbeatTimer();
    }

    /**
     * 复制logEntry
     * @param command
     * @return
     */
    public CompletableFuture<Boolean>  replicateEntry(Command command){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (state != NodeState.LEADER) {
            logger.debug("I'm not the leader");
            future.complete(false);
            return future;
        }

        //1. 将日志写入本地日志，并复制到所有节点
        long replicateEntryIndex = writeLogEntry(command);

        //2. 如果配置中不要求写入大部分节点，则直接返回
        if (raftConfig.isAsyncWrite()) {
            future.complete(true);
            return future;// 主节点写成功后，就返回。
        }

        //3.记录还未复制到大部分节点的future
        replicateEntryFutureMap.put(replicateEntryIndex, future);

        return future;

    }

    public long writeLogEntry(Command command){
        LogEntry logEntry = new LogEntry();
        logEntry.setTerm(currentTerm);
        logEntry.setCommand(command);
        long logEntryIndex = raftLogStorage.write(logEntry);
        if (logEntry.getCommand() instanceof UserDefinedCommand){
            for(UserDefinedCommandListener userDefinedCommandListener : userDefinedCommandListeners){
                userDefinedCommandListener.onWrite((UserDefinedCommand) logEntry.getCommand(), logEntryIndex, currentTerm);
            }
        }
        return logEntryIndex;
    }

    /**
     * 1. 检查是否要进行install snapshot
     * 2. 创建AppendEntriesRequest
     * 3. 发送appendEntriesRequest
     */
    private void appendEntries(NodePeer nodePeer){
        //1. 检查是否要进行install snapshot
        boolean isNeedInstallSnapshot = false;
        raftLogStorage.getWriteLock().lock();
        try {
            long firstLogIndex = raftLogStorage.getLastIndex() - raftLogStorage.getTotalSize() + 1;

            if (nodePeer.getNextIndex() < firstLogIndex){
                isNeedInstallSnapshot = true;
            }
        }finally {
            raftLogStorage.getWriteLock().unlock();
        }

        if (isNeedInstallSnapshot) {
            logger.info("is need snapshot={}, nodePeer={}", isNeedInstallSnapshot, nodePeer.getPeerAddress());
            //等待install 快照完成
            if (!installSnapshot(nodePeer)) {
                return;
            }
        }

        //2. 创建AppendEntriesRequest
        AppendEntriesRequest appendEntriesRequest = AppendEntriesRequest.builder().build();
        readLock.lock();
        try {
            long prevLogIndex = nodePeer.getNextIndex() - 1;
            long prevLogTerm;
            // 判断prevLogTerm能否直接从日志中获得
            if (prevLogIndex == 0)
                prevLogTerm = 0;
            else if(prevLogIndex == raftMetaStorage.getSnapshotLastIncludedIndex())
                prevLogTerm = raftMetaStorage.getSnapshotLastIncludedTerm();
            else
                prevLogTerm = raftLogStorage.read(prevLogIndex).getTerm();

            appendEntriesRequest.setGroupId(groupId);
            appendEntriesRequest.setLeaderId(serverId);
            appendEntriesRequest.setLeaderTerm(currentTerm);
            appendEntriesRequest.setPrevLogIndex(prevLogIndex);
            appendEntriesRequest.setPrevLogTerm(prevLogTerm);
            long lastIndex = Math.min(raftLogStorage.getLastIndex(), nodePeer.getNextIndex() + raftConfig.getMaxLogEntriesPerRequest() - 1);
            List<LogEntry> logEntries = new ArrayList<>();
            for (long index = nodePeer.getNextIndex(); index <= lastIndex; index++) {
                logEntries.add(raftLogStorage.read(index));
            }
            appendEntriesRequest.setEntries(logEntries.size() == 0 ? null : logEntries);
            appendEntriesRequest.setLeaderCommit(Math.min(commitIndex, prevLogIndex + logEntries.size()));//不将未复制的日志条目的提交状态发送给follower节点
        } finally {
            readLock.unlock();
        }
        //3. 发送appendEntriesRequest
        nodePeer.appendEntries(appendEntriesRequest, new AppendEntriesResponseCallback(nodePeer, appendEntriesRequest));

    }


    private class AppendEntriesResponseCallback implements RpcResponseCallBack{

        private NodePeer nodePeer;
        private AppendEntriesRequest appendEntriesRequest;

        public AppendEntriesResponseCallback(NodePeer nodePeer, AppendEntriesRequest appendEntriesRequest) {
            this.nodePeer = nodePeer;
            this.appendEntriesRequest = appendEntriesRequest;
        }

        /**
         * 1. 如果appendEntriesResponse的任期大于当前节点任期，则处理任期落后
         * 2. 如果success为true，即跟随者所含有的条目和preLogIndex以及preLogTerm匹配上，则设置peer的matchIndex和nextIndex，并尝试AdvanceCommitIndex
         * 3. 如果success为false，则将peer的nextIndex向后退一位
         * @param data
         */
        @Override
        public void onResponse(Object data) {
            AppendEntriesResponse appendEntriesResponse = (AppendEntriesResponse)data;
            if (appendEntriesResponse.getCurrentTerm() > currentTerm)
                handleTermLag(appendEntriesResponse.getCurrentTerm());
            else {
                if (appendEntriesResponse.isSuccess()){
                    int deltaSize = appendEntriesRequest.getEntries() == null ? 0 : appendEntriesRequest.getEntries().size();
                    nodePeer.setMatchIndex(appendEntriesRequest.getPrevLogIndex() + deltaSize);
                    nodePeer.setNextIndex(nodePeer.getMatchIndex() + 1);
                    mayAdvanceCommitIndex();
                }else {
                    if (nodePeer.getNextIndex() > 1) nodePeer.setNextIndex(nodePeer.getNextIndex() - 1);
                }
            }

        }

        @Override
        public void onException(Throwable throwable) {
            logger.error("appendEntries with nodePeer[{}] failed : {}", nodePeer.getPeerAddress(), throwable.getMessage());
        }
    }

    /**
     * 1. 通过各节点matchIndex判断最大的newCommitIndex
     * 2. 判断newCommitIndex是否有效(leader不能提交之前的任期内的日志)
     * 3. newCommitIndex有效，更改节点commitIndex，并持久化。并将新提交的日志条目应用到状态机中
     */
    private void mayAdvanceCommitIndex(){

        writeLock.lock();
        try {
            //1. 通过各节点matchIndex判断最大的newCommitIndex
            List<Long> matchIndexes = new ArrayList<>();
            matchIndexes.add(raftLogStorage.getLastIndex());
            for(NodePeer nodePeer : peers.values()){
                matchIndexes.add(nodePeer.getMatchIndex());
            }
            Collections.sort(matchIndexes);

            long newCommitIndex = matchIndexes.get((matchIndexes.size() - 1) / 2);
            logger.debug("newCommitIndex={}, oldCommitIndex={}", newCommitIndex, commitIndex);

            //2. 判断newCommitIndex是否有效
            if (newCommitIndex != 0 && raftLogStorage.read(newCommitIndex).getTerm() != currentTerm) {
                //leader不能提交之前的任期内的日志
                logger.debug("newCommitIndexTerm={}, currentTerm={}", raftLogStorage.read(newCommitIndex).getTerm(), currentTerm);
                return;
            }
            if (commitIndex >= newCommitIndex) {
                return;
            }

            //3. newCommitIndex有效，更改节点commitIndex，并持久化。并将新提交的日志条目应用到状态机中
            long oldLastAppliedIndex = lastAppliedIndex;
            commitIndex = newCommitIndex;

            for (long index = oldLastAppliedIndex + 1; index <= newCommitIndex; index++) {
                LogEntry logEntry = raftLogStorage.read(index);
                applyCommand(logEntry.getCommand());
                lastAppliedIndex = index;
                raftMetaStorage.setLastAppliedIndex(lastAppliedIndex);
            }
            //4. 唤醒所有等待commitIndex的future

            logger.info("commitIndex={} lastAppliedIndex={}", commitIndex, lastAppliedIndex);
            for(Long i = oldLastAppliedIndex + 1; i <= commitIndex && replicateEntryFutureMap.containsKey(i); i++){
                replicateEntryFutureMap.remove(i).complete(true);
            }
        }finally {
            writeLock.unlock();
        }


    }


    /**---------------------------------------快照与恢复-----------------------------------**/

    public boolean installSnapshot(NodePeer nodePeer) {
        //1. 一些取消installSnapshot的情况
        if (isTakeSnapshot.get()) {
            logger.info("already in take snapshot, please send install snapshot request later");
            return false;
        }

        if (!isInstallSnapshot.compareAndSet(false, true)) {
            logger.info("already in install snapshot");
            return false;
        }

        try {
            boolean isLastRequest = false;
            int offset = 0;
            boolean isLastFile = false;
            //2. 获取快照数据
            Map<String, byte[]> snapshotFileData = MyFileUtils.readAllFiles(nodeConfig.getSnapshotDir());

            //3. 将所有的快照数据分块发送
            int fileCount = 0;
            for(Map.Entry<String, byte[]> entry : snapshotFileData.entrySet()){

                if (++fileCount == snapshotFileData.size()) isLastFile = true;
                String fileName = entry.getKey();
                byte[] fileData = entry.getValue();
                int fileDataOffset = 0;
                while (fileDataOffset < fileData.length){
                    int snapshotBytesThisRequest;
                    if (fileData.length - fileDataOffset <= raftConfig.getMaxSnapshotBytesPerRequest()){
                        snapshotBytesThisRequest = fileData.length - fileDataOffset;
                        isLastRequest = isLastFile ? true : false;
                    }else
                        snapshotBytesThisRequest = raftConfig.getMaxSnapshotBytesPerRequest();

                    byte[] snapshotBytes = Arrays.copyOfRange(fileData, fileDataOffset, fileDataOffset + snapshotBytesThisRequest);
                    InstallSnapshotRequest installSnapshotRequest = InstallSnapshotRequest.builder()
                            .groupId(groupId)
                            .snapshotFileName(fileName)
                            .data(snapshotBytes)
                            .leaderId(leaderId)
                            .leaderTerm(currentTerm)
                            .lastIncludedIndex(raftMetaStorage.getSnapshotLastIncludedIndex())
                            .lastIncludedTerm(raftMetaStorage.getSnapshotLastIncludedTerm())
                            .offset(offset)
                            .done(isLastRequest)
                            .build();

                    InstallSnapshotResponse installSnapshotResponse = nodePeer.getRaftConsensusServiceSync().installSnapshot(installSnapshotRequest);
                    if (installSnapshotResponse == null)
                        return false;
                    else if (installSnapshotResponse.getCurrentTerm() > currentTerm){
                        handleTermLag(installSnapshotResponse.getCurrentTerm());
                        return false;
                    }else if (!installSnapshotResponse.isSuccess()){
                        return false;
                    }

                    fileDataOffset += snapshotBytesThisRequest;
                    offset += snapshotBytesThisRequest;

                }
            }

            nodePeer.setNextIndex(raftMetaStorage.getSnapshotLastIncludedIndex() + 1);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("install snapshot failed ：" + e);
            return false;
        }finally {
            isInstallSnapshot.compareAndSet(true, false);
        }
        return true;
    }

    /**
     * 每个服务器独立的创建快照，快照内容：
     * 1.已经被提交的日志，即状态机的状态
     * 2.元数据：last included index（状态机最后应用的日志的索引）、last included term（状态机最后应用的日志的任期）、raft配置
     */
    public void takeSnapshot(){
        //1.一些取消takeSnapshot的情况
        if (isInstallSnapshot.get()) {
            logger.info("the node is already in install snapshot, ignore take snapshot");
            return;
        }
        if (raftLogStorage.getTotalSize() < raftConfig.getSnapshotMinLogSize()) {

            logger.info("the log is smaller than snapshotMinLogSize, ignore take snapshot");
            return;
        }
        if (!isTakeSnapshot.compareAndSet(false, true)){
            logger.info("the node is already taking snapshot, ignore take snapshot again");
            return;
        }

        try {
            //2.takeSnapshot时可能在复制日志，应用日志等，所以要采用写时复制
            logger.info("start taking snapshot");
            boolean success = false;
            String snapshotDir = nodeConfig.getSnapshotDir();
            String tmpSnapshotDir = snapshotDir +  ".tmp";
            File snapshotDirFile = new File(snapshotDir);
            File tmpSnapshotDirFile = new File(tmpSnapshotDir);

            writeLock.lock();
            long lastIncludedIndex = 0;
            long lastIncludeTerm = 0;
            try {
                lastIncludedIndex = lastAppliedIndex;
                lastIncludeTerm = raftLogStorage.read(lastIncludedIndex).getTerm();
                //3.先将snapshot写入临时文件夹，避免take snapshot失败，覆盖了以前的snapshot
                FileUtils.deleteDirectory(tmpSnapshotDirFile);
                //3.1 take config snapshot
                raftConfig.writeSnapshot(tmpSnapshotDir);
                //3.2 take statemachine snapshot
                raftStateMachine.writeSnapshot(tmpSnapshotDir);
            }catch (Exception e){
                e.printStackTrace();
                logger.error("taking snapshot cause error ：" + e);
            }finally{
                writeLock.unlock();
            }

            try {
                //4 将tmpSnapshotDir覆盖snapshotDirPath
                FileUtils.deleteDirectory(snapshotDirFile);
                FileUtils.moveDirectory(tmpSnapshotDirFile, snapshotDirFile);
                success = true;
                logger.info("end taking snapshot : success!");
            }catch (IOException e){
                e.printStackTrace();
                logger.error("taking snapshot cause error ：" + e);
            }

            //5. 如果take snapshot成功，则丢弃旧的日志条目, 更新快照元数据
            if (success){
                raftLogStorage.removeToEndIndex(lastIncludedIndex);
                raftMetaStorage.setSnapshotLastIncludedIndex(lastIncludedIndex);
                raftMetaStorage.setSnapshotLastIncludedTerm(lastIncludeTerm);
            }

        }finally {
            isTakeSnapshot.compareAndSet(true, false);
        }
    }

    /**
     * 加载快照和日志，恢复上一次宕机的状态
     */
    private void recover(){
        loadMeta();
        loadSnapshot();
        loadRaftLog();
        logger.info(this.toString());


    }

    private void loadMeta(){
        writeLock.lock();
        try {
            votedFor = raftMetaStorage.getVotedFor();
            currentTerm = raftMetaStorage.getCurrentTerm();
            lastAppliedIndex = raftMetaStorage.getLastAppliedIndex();
            commitIndex = lastAppliedIndex;
        }finally {
            writeLock.unlock();
        }
    }

    /**
     * 因为此方法节点初始化时调用，所以不需要加锁
     * 节点重启时读取Snapshot
     *
     * 1.读取Snapshot，恢复Node中的状态变量的commitIndex、currentTerm、lastAppliedIndex以及配置raftConfig
     * 2.状态机读取Snapshot
     * @throws Exception
     *
     */
    public void loadSnapshot()  {
        writeLock.lock();
        try {
            raftStateMachine.readSnapshot(nodeConfig.getSnapshotDir());
            raftConfig.readSnapshot(nodeConfig.getSnapshotDir());
            updatePeers();
            lastAppliedIndex = Math.max(lastAppliedIndex, raftMetaStorage.getSnapshotLastIncludedIndex());
            commitIndex = Math.max(commitIndex, raftMetaStorage.getSnapshotLastIncludedIndex());
        }catch (Exception e){
            //e.printStackTrace();
            throw new RaftException(RaftError.LOAD_SNAPSHOT_ERROR, e.toString());
        }finally {
            writeLock.unlock();
        }

    }

    private void loadRaftLog(){
        writeLock.lock();
        try {
            for(long index = raftMetaStorage.getSnapshotLastIncludedIndex() + 1; index <= lastAppliedIndex; index++){
                LogEntry logEntry = raftLogStorage.read(index);
                applyCommand(logEntry.getCommand());
            }
        }finally {
            writeLock.unlock();
        }

    }


    /**----------------------------------集群成员以及配置变更------------------------------**/

    /**
     * 更改日志条目中对应的配置
     * https://segmentfault.com/a/1190000022796386
     * https://www.cnblogs.com/foxmailed/p/7190642.html
     */
    public void applyMemberConfiguration(MemberConfigCommand command) {
        if(command.getType() == MemberConfigCommand.Type.ADD)
            raftConfig.getRaftGroupAddress().put(command.getKey(), command.getValue());
        else if (command.getType() == MemberConfigCommand.Type.REMOVE)
            raftConfig.getRaftGroupAddress().remove(command.getKey());

        updatePeers();

        logger.info("set conf :{} = {}, leaderId={}", command.getKey(), command.getValue(), leaderId);
    }

    /**---------------------------------utils method-------------------------------**/

    public void applyCommand(Command command){

        if (command instanceof EmptyCommand)
            return;
        if (command instanceof SMCommand)
            raftStateMachine.apply((SMCommand) command);
        else if (command instanceof MemberConfigCommand)
            applyMemberConfiguration((MemberConfigCommand) command);
        else if (command instanceof UserDefinedCommand){
            for(UserDefinedCommandListener userDefinedCommandListener : userDefinedCommandListeners)
                userDefinedCommandListener.onApply((UserDefinedCommand) command);
        }
    }



    /**
     * 当发现有别的节点的Term大于此节点的term时，进行处理
     *
     * 1.变成follower状态
     * 2.修改currentTerm、leaderId、votedFor等状态变量
     * 3.如果是leader的话，需要stop heartbeat
     * 4.重置选举计时器
     * @return
     */
    public void handleTermLag(long newTerm){
        if (currentTerm >= newTerm) {
            return;
        }
        writeLock.lock();
        try {
            logger.info("handle term lagging : myTerm {}, newTerm {}", currentTerm, newTerm);
            state = NodeState.FOLLOWER;
            currentTerm = newTerm;
            leaderId = 0;
            votedFor = 0;
            raftMetaStorage.setVotedFor(votedFor);
            raftMetaStorage.setCurrentTerm(currentTerm);

            // 如果是leader的话，需要停止心跳计时器
            if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
                heartbeatScheduledFuture.cancel(true);
            }
        }finally {
            writeLock.unlock();
            //开启选举定时器
            resetElectionTimer();
        }


    }


    public long getLastLogTerm(){
        LogEntry lastLogEntry = raftLogStorage.getLast();
        return lastLogEntry == null? raftMetaStorage.getSnapshotLastIncludedTerm(): lastLogEntry.getTerm();//当生成快照后，日志为空。
    }

    public boolean isLeader(){
        return state.equals(NodeState.LEADER);
    }

    public void addUserDefinedCommandListener(UserDefinedCommandListener userDefinedCommandListener){
        userDefinedCommandListeners.add(userDefinedCommandListener);
    }

    public void removeUserDefinedCommandListener(UserDefinedCommandListener userDefinedCommandListener){
        userDefinedCommandListeners.remove(userDefinedCommandListener);
    }

    @Override
    public String toString() {
        return "RaftNode{" +
                "state=" + state +
                ", currentTerm=" + currentTerm +
                ", votedFor=" + votedFor +
                ", leaderId=" + leaderId +
                ", commitIndex=" + commitIndex +
                ", lastAppliedIndex=" + lastAppliedIndex +
                ", serverId=" + serverId +
                ", groupId=" + groupId +
                ", raftConfig=" + raftConfig +
                '}';
    }
}
