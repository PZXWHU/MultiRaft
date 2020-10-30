package com.pzx.raft.node;

import com.alibaba.fastjson.JSONObject;
import com.pzx.raft.config.NodeConfig;
import com.pzx.raft.config.RaftConfig;
import com.pzx.raft.log.Command;
import com.pzx.raft.log.LogEntry;
import com.pzx.raft.exception.RaftError;
import com.pzx.raft.exception.RaftException;
import com.pzx.raft.log.RocksDBRaftLog;
import com.pzx.raft.service.RaftClusterMembershipChangeService;
import com.pzx.raft.service.RaftConsensusService;
import com.pzx.raft.service.RaftKVClientService;
import com.pzx.raft.service.entity.*;
import com.pzx.raft.service.impl.RaftClusterMembershipChangeServiceImpl;
import com.pzx.raft.service.impl.RaftConsensusServiceImpl;
import com.pzx.raft.service.impl.RaftKVClientServiceImpl;
import com.pzx.raft.statemachine.MemoryMapStateMachine;
import com.pzx.raft.statemachine.StateMachine;
import com.pzx.raft.log.RaftLog;
import com.pzx.raft.snapshot.SnapshotMetaData;
import com.pzx.raft.utils.AddressUtils;
import com.pzx.raft.utils.MyFileUtils;
import com.pzx.raft.utils.ThreadPoolUtils;
import com.pzx.rpc.invoke.RpcResponseCallBack;
import com.pzx.rpc.transport.RpcServer;
import com.pzx.rpc.transport.netty.server.NettyServer;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private ConcurrentMap<Integer, Peer> peerMap = new ConcurrentHashMap<>();

    //Raft日志:每个条目包含了用于状态机的命令、领导者接收到该条目时的任期以及条目在日志中的索引
    private RaftLog raftLog;

    //状态机
    private StateMachine stateMachine;

    //节点的状态，起始为follower
    private NodeState state = NodeState.FOLLOWER;

    //服务器已知最新的任期（在服务器首次启动的时候初始化为0，单调递增）
    private long currentTerm = 0;

    //当前任期内收到选票的候选者id 如果没有投给任何候选者 则为0(要求不能有节点id为0)
    private int votedFor = 0;

    //目前集群中leader节点的id
    private int leaderId = 0;

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

    //表示最近一次进行快照的元数据
    private SnapshotMetaData snapshotMetaData = new SnapshotMetaData();

    //表示选举过程的ScheduledFuture
    private ScheduledFuture electionScheduledFuture;

    //表示heartbeat的ScheduledFuture
    private ScheduledFuture heartbeatScheduledFuture;

    //RaftNode内部锁，当对节点内部状态变量、日志、状态机、配置进行改变时，必须加锁以保证整体状态的一致性
    private Lock lock = new ReentrantLock();

    //commitIndex条件对象，当commitIndex符合条件时，唤醒所有线程
    private Condition commitIndexCondition = lock.newCondition();

    //RaftNode发送心跳、请求投票、安装快照等动作的线程池
    private ExecutorService  executorService ;

    //RaftNode触发心跳、选举的scheduled线程池
    private ScheduledExecutorService scheduledExecutorService;

    //RaftNode提供RPC远程调用的server
    private RpcServer rpcServer;

    public RaftNode(String configPath){

        this.raftConfig = initConfig(configPath);
        this.stateMachine = new MemoryMapStateMachine();
        this.raftLog = new RocksDBRaftLog(NodeConfig.getLogDir());

        recover();//恢复上一次宕机的状态

        //初始化同伴节点
        for(Map.Entry<Integer, String> entry : raftConfig.getPeersAddress().entrySet()){
            peerMap.put(entry.getKey(), new Peer(entry.getKey(), entry.getValue(), raftLog.getLastIndex() + 1));
        }

        executorService = ThreadPoolUtils.newThreadPoolExecutor(raftConfig.getRaftConsensusThreadNum(), raftConfig.getRaftConsensusThreadNum(),
                60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        scheduledExecutorService = ThreadPoolUtils.getScheduledThreadPool();

        rpcServer = initRpcServer();
    }

    public void start(){
        //重置选举定时器
        resetElectionTimer();
        //开启快照任务
        scheduledExecutorService.scheduleWithFixedDelay(()->takeSnapshot(),
                        raftConfig.getSnapshotPeriodSeconds(), raftConfig.getSnapshotPeriodSeconds(), TimeUnit.SECONDS);
        //开启RpcServer
        rpcServer.start();//此方法会阻塞，所以必须放在最后
    }

    /**---------------------------------------初始化----------------------------**/

    private RaftConfig initConfig(String configPath){
        RaftConfig raftConfig;
        try {
            String jsonString = MyFileUtils.readFileToString(configPath);
            JSONObject jsonObject = JSONObject.parseObject(jsonString);
            NodeConfig.nodeId = (int)jsonObject.get(NodeConfig.NODE_ID_CONFIG_NAME);
            NodeConfig.raftHome = (String)jsonObject.get(NodeConfig.RAFT_HOME_CONFIG_NAME);
            MyFileUtils.mkDirIfNotExist(NodeConfig.raftHome);//创建Raft主目录
            raftConfig = JSONObject.parseObject(jsonString, RaftConfig.class);
        }catch (IOException e){
            throw new RaftException(RaftError.LOAD_SNAPSHOT_ERROR, e.getMessage());
        }
        return raftConfig;
    }

   private RpcServer initRpcServer(){
       InetSocketAddress selfAddress = AddressUtils.stringToInetSocketAddress(raftConfig.getSelfAddress());
       RpcServer rpcServer = new NettyServer.Builder(selfAddress).autoScanService(false).build();
       rpcServer.publishService(new RaftConsensusServiceImpl(this), RaftConsensusService.class.getCanonicalName());
       rpcServer.publishService(new RaftKVClientServiceImpl(this), RaftKVClientService.class.getCanonicalName());
       rpcServer.publishService(new RaftClusterMembershipChangeServiceImpl(this), RaftClusterMembershipChangeService.class.getCanonicalName());
       return rpcServer;
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
     * 开始选举：
     *  1.转变成候选人状态
     *  2.自增当前的任期号（currentTerm）
     *  3.给自己投票
     *  4.重置选举超时计时器
     *  5.发送请求投票的 RPC 给其他所有服务器
     */
    private void startNewElection(){

        lock.lock();
        try {
            if (!raftConfig.getClusterAddress().containsKey(NodeConfig.nodeId)){
                resetElectionTimer();
                return;
            }
            leaderId = 0;//重新选举，消除领导者

            state = NodeState.CANDIDATE;
            currentTerm++;
            logger.info("Running for election in term {}", currentTerm);
            //重置选举超时计时器
            resetElectionTimer();
            votedFor = NodeConfig.nodeId;
            voteGrantedNum.set(1);//清除之前的投票计数，新的任期目前只有自己投票
            raftLog.updateNodePersistMetaData(NodePersistMetadata.builder().votedFor(votedFor).currentTerm(currentTerm).build());

        }finally {
            lock.unlock();
        }

        for (Peer peer : peerMap.values()){
            executorService.submit(()->requestVote(peer));
        }

    }

    private void requestVote(Peer peer){
        ReqVoteRequest reqVoteRequest = ReqVoteRequest.builder()
                .candidateId(NodeConfig.nodeId)
                .candidateTerm(currentTerm)
                .lastLogIndex(getLastLogIndex())
                .lastLogTerm(getLastLogTerm())
                .build();
        peer.requestVote(reqVoteRequest, new ReqVoteResponseCallback(peer, reqVoteRequest));
    }

    private class ReqVoteResponseCallback implements RpcResponseCallBack{

        private final Peer peer;
        private final ReqVoteRequest reqVoteRequest;

        public ReqVoteResponseCallback(Peer peer, ReqVoteRequest reqVoteRequest) {
            this.peer = peer;
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
            lock.lock();
            try {
                if (currentTerm != reqVoteRequest.getCandidateTerm() || state != NodeState.CANDIDATE) {
                    logger.info("ignore requestVote RPC result");
                    return;
                }
                if (reqVoteResponse.getCurrentTerm() > currentTerm){
                    logger.info("Received RequestVote response from server {} in term {} (this server's term was {})",
                            peer.getNodeId(), reqVoteResponse.getCurrentTerm(), currentTerm);
                    handleTermLag(reqVoteResponse.getCurrentTerm());
                }else {
                    if (reqVoteResponse.isVoteGranted()){

                        int currentVoteGrantedNum  = voteGrantedNum.incrementAndGet();
                        logger.info("Got vote from server {} for term {}, currentVoteGrantedNum={}", peer.getNodeId(), currentTerm, currentVoteGrantedNum);
                        if (currentVoteGrantedNum > raftConfig.getClusterSize() / 2){
                            logger.info("Got majority vote, nodeId={} become leader", NodeConfig.nodeId);
                            becomeLeader();
                        }
                    }
                }
            }finally {
                lock.unlock();
            }

        }
        @Override
        public void onException(Throwable throwable) {
            logger.error("requestVote with peer[{}] failed : {}", peer.getPeerAddress(), throwable.getMessage());
        }
    }

    private void becomeLeader(){
        lock.lock();
        try {
            state = NodeState.LEADER;
            leaderId = NodeConfig.nodeId;
            //当一个领导人刚获得权力的时候，他初始化所有的 nextIndex 值为自己的最后一条日志的 index 加 1
            for(Peer peer : peerMap.values()){
                peer.setNextIndex(raftLog.getLastIndex() + 1);
            }
        }finally {
            lock.unlock();
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
        logger.info("start new heartbeat, peers={}", peerMap.keySet());
        for (Peer peer : peerMap.values()) {
            executorService.submit(()->appendEntries(peer));//发送心跳
        }
        resetHeartbeatTimer();
    }

    /**
     * 将logEntry写入本地日志，并复制到大部分节点
     * @param logEntry
     * @return
     */
    public boolean replicateEntry(LogEntry logEntry){
        if (state != NodeState.LEADER) {
            logger.debug("I'm not the leader");
            return false;
        }
        lock.lock();
        try {
            //1. 将日志写入本地日志，并复制到所有节点
            long replicateEntryIndex = raftLog.write(logEntry);

            //3. 如果配置中不要求写入大部分节点，则直接返回
            if (raftConfig.isAsyncWrite()) {
                return true;// 主节点写成功后，就返回。
            }
            //4. 等待commitIndex达到复制日志的index，或者达到最大时长
            try {
                long startTime = System.currentTimeMillis();
                while (lastAppliedIndex < replicateEntryIndex) {
                    if (System.currentTimeMillis() - startTime >= raftConfig.getMaxAwaitTimeout()) {
                        break;
                    }
                    commitIndexCondition.await(raftConfig.getMaxAwaitTimeout(), TimeUnit.MILLISECONDS);
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }

            //5. 如果等待过程被中断，或者达到最大时长，但是并没有复制到大多数节点，则返回写入失败
            logger.debug("lastAppliedIndex={} replicateEntryIndex={}", lastAppliedIndex, replicateEntryIndex);
            if (lastAppliedIndex < replicateEntryIndex) {
                return false;
            }
            return true;
        }finally {
            lock.unlock();
        }



    }

    /**
     * 1. 检查是否要进行install snapshot
     * 2. 创建AppendEntriesRequest
     * 3. 发送appendEntriesRequest
     */
    private void appendEntries(Peer peer){
        //1. 检查是否要进行install snapshot
        boolean isNeedInstallSnapshot = false;
        raftLog.getLock().lock();
        try {
            long firstLogIndex = raftLog.getLastIndex() - raftLog.getTotalSize() + 1;
            if (peer.getNextIndex() < firstLogIndex){
                isNeedInstallSnapshot = true;
            }

        }finally {
            raftLog.getLock().unlock();
        }

        if (isNeedInstallSnapshot) {
            logger.info("is need snapshot={}, peer={}", isNeedInstallSnapshot, peer.getPeerAddress());
            //等待install 快照完成
            if (!installSnapshot(peer)) {
                return;
            }
        }

        //2. 创建AppendEntriesRequest
        AppendEntriesRequest appendEntriesRequest = AppendEntriesRequest.builder().build();
        lock.lock();
        try {
            long prevLogIndex = peer.getNextIndex() - 1;
            long prevLogTerm;
            // 判断prevLogTerm能否直接从日志中获得
            if (prevLogIndex == 0)
                prevLogTerm = 0;
            else if(prevLogIndex == snapshotMetaData.getLastIncludedIndex())
                prevLogTerm = snapshotMetaData.getLastIncludedTerm();
            else
                prevLogTerm = raftLog.read(prevLogIndex).getTerm();

            appendEntriesRequest.setLeaderId(NodeConfig.nodeId);
            appendEntriesRequest.setLeaderTerm(currentTerm);
            appendEntriesRequest.setPrevLogIndex(prevLogIndex);
            appendEntriesRequest.setPrevLogTerm(prevLogTerm);
            long lastIndex = Math.min(raftLog.getLastIndex(), peer.getNextIndex() + raftConfig.getMaxLogEntriesPerRequest() - 1);
            List<LogEntry> logEntries = new ArrayList<>();
            for (long index = peer.getNextIndex(); index <= lastIndex; index++) {
                logEntries.add(raftLog.read(index));
            }
            appendEntriesRequest.setEntries(logEntries.size() == 0 ? null : logEntries);
            appendEntriesRequest.setLeaderCommit(Math.min(commitIndex, prevLogIndex + logEntries.size()));//不将未复制的日志条目的提交状态发送给follower节点
        } finally {
            lock.unlock();
        }
        //3. 发送appendEntriesRequest
        peer.appendEntries(appendEntriesRequest, new AppendEntriesResponseCallback(peer, appendEntriesRequest));

    }


    private class AppendEntriesResponseCallback implements RpcResponseCallBack{

        private Peer peer;
        private AppendEntriesRequest appendEntriesRequest;

        public AppendEntriesResponseCallback(Peer peer, AppendEntriesRequest appendEntriesRequest) {
            this.peer = peer;
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
                    peer.setMatchIndex(appendEntriesRequest.getPrevLogIndex() + appendEntriesRequest.getEntries().size());
                    peer.setNextIndex(peer.getMatchIndex() + 1);
                    mayAdvanceCommitIndex();
                }else
                    if (peer.getNextIndex() > 1)
                        peer.setNextIndex(peer.getNextIndex() - 1);
            }
        }

        @Override
        public void onException(Throwable throwable) {
            logger.error("appendEntries with peer[{}] failed : {}", peer.getPeerAddress(), throwable.getMessage());
        }
    }

    /**
     * 1. 通过各节点matchIndex判断最大的newCommitIndex
     * 2. 判断newCommitIndex是否有效(leader不能提交之前的任期内的日志)
     * 3. newCommitIndex有效，更改节点commitIndex，并持久化。并将新提交的日志条目应用到状态机中
     */
    private void mayAdvanceCommitIndex(){

        lock.lock();
        try {
            //1. 通过各节点matchIndex判断最大的newCommitIndex
            List<Long> matchIndexes = new ArrayList<>();
            matchIndexes.add(raftLog.getLastIndex());
            for(Peer peer : peerMap.values()){
                matchIndexes.add(peer.getMatchIndex());
            }
            Collections.sort(matchIndexes);
            long newCommitIndex = matchIndexes.get((matchIndexes.size() - 1) / 2);
            logger.debug("newCommitIndex={}, oldCommitIndex={}", newCommitIndex, commitIndex);

            //2. 判断newCommitIndex是否有效
            if (raftLog.read(newCommitIndex).getTerm() != currentTerm) {
                //leader不能提交之前的任期内的日志
                logger.debug("newCommitIndexTerm={}, currentTerm={}", raftLog.read(newCommitIndex).getTerm(), currentTerm);
                return;
            }
            if (commitIndex >= newCommitIndex) {
                return;
            }

            //3. newCommitIndex有效，更改节点commitIndex，并持久化。并将新提交的日志条目应用到状态机中
            long oldCommitIndex = commitIndex;
            commitIndex = newCommitIndex;
            raftLog.updateNodePersistMetaData(NodePersistMetadata.builder().commitIndex(commitIndex).build());
            for (long index = oldCommitIndex + 1; index <= newCommitIndex; index++) {
                LogEntry logEntry = raftLog.read(index);
                if (logEntry.getCommand().getCommandType() == Command.CommandType.DATA)
                    stateMachine.apply(logEntry);
                else if (logEntry.getCommand().getCommandType() == Command.CommandType.CONFIGURATION)
                    applyConfiguration(logEntry);
                lastAppliedIndex = index;
            }
            //4. 唤醒所有等待commitIndex条件对象的线程

            logger.info("commitIndex={} lastAppliedIndex={}", commitIndex, lastAppliedIndex);
            commitIndexCondition.signalAll();
        }finally {
            lock.unlock();
        }


    }


    /**---------------------------------------快照与恢复-----------------------------------**/

    private boolean installSnapshot(Peer peer) {
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
            Map<String, byte[]> snapshotFileData = stateMachine.getSnapshotFileData(NodeConfig.getSnapshotDir());
            snapshotFileData.putAll(snapshotMetaData.getSnapshotFileData(NodeConfig.getSnapshotDir()));

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
                            .snapshotFileName(fileName)
                            .data(snapshotBytes)
                            .leaderId(leaderId)
                            .leaderTerm(currentTerm)
                            .lastIncludedIndex(snapshotMetaData.getLastIncludedIndex())
                            .lastIncludedTerm(snapshotMetaData.getLastIncludedTerm())
                            .offset(offset)
                            .done(isLastRequest)
                            .build();

                    InstallSnapshotResponse installSnapshotResponse = peer.getRaftConsensusServiceSync().installSnapshot(installSnapshotRequest);
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

            peer.setNextIndex(snapshotMetaData.getLastIncludedIndex() + 1);
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
        if (raftLog.getTotalSize() < raftConfig.getSnapshotMinLogSize()) {
            logger.info("the log is smaller than snapshotMinLogSize, ignore take snapshot");
            return;
        }
        if (!isTakeSnapshot.compareAndSet(false, true)){
            logger.info("the node is already taking snapshot, ignore take snapshot again");
            return;
        }

        try {
            //2.takeSnapshot时可能在复制日志，应用日志等，所以要采用写时复制
            SnapshotMetaData newSnapshotMetaData;
            StateMachine copyStateMachine;
            lock.lock();
            try {
                RaftConfig copyConfig = raftConfig.copy();
                long lastIncludedIndex = lastAppliedIndex;
                long lastIncludedTerm = raftLog.read(lastIncludedIndex).getTerm();
                newSnapshotMetaData = SnapshotMetaData.builder()
                        .lastIncludedIndex(lastIncludedIndex)
                        .lastIncludedTerm(lastIncludedTerm)
                        .raftConfig(copyConfig)
                        .build();
                copyStateMachine = stateMachine.copy();
            }finally {
                lock.unlock();
            }

            logger.info("start taking snapshot");
            boolean success = false;
            String snapshotDir = NodeConfig.getSnapshotDir();
            String tmpSnapshotDir = snapshotDir +  ".tmp";

            try {
                //3.先将snapshot写入临时文件夹，避免take snapshot失败，覆盖了以前的snapshot
                //3.1 take metadata snapshot
                newSnapshotMetaData.writeSnapshot(tmpSnapshotDir);
                //3.2 take statemachine snapshot
                copyStateMachine.writeSnapshot(tmpSnapshotDir);

                //4 将tmpSnapshotDir覆盖snapshotDir
                MyFileUtils.deleteDirIfExist(snapshotDir);
                MyFileUtils.moveDirectory(tmpSnapshotDir, snapshotDir);

                success = true;
                logger.info("end taking snapshot : success!");
            }catch (Exception e){
                logger.error("taking snapshot cause error ：" + e);
            }

            //5. 如果take snapshot成功，则丢弃旧的日志条目, 更新快照元数据
            if (success){
                raftLog.removeToEndIndex(newSnapshotMetaData.getLastIncludedIndex());
                this.snapshotMetaData = newSnapshotMetaData ;
            }

        }finally {
            isTakeSnapshot.compareAndSet(true, false);
        }
    }

    /**
     * 加载快照和日志，恢复上一次宕机的状态
     */
    private void recover(){
        loadSnapshot();
        loadRaftLog();
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
        try {
            snapshotMetaData.readSnapshot(NodeConfig.getSnapshotDir());
            commitIndex = Math.max(commitIndex, snapshotMetaData.getLastIncludedIndex());
            currentTerm = Math.max(currentTerm, snapshotMetaData.getLastIncludedTerm());
            lastAppliedIndex = commitIndex;
            raftConfig = snapshotMetaData.getRaftConfig() == null ? raftConfig : snapshotMetaData.getRaftConfig();
            stateMachine.readSnapshot(NodeConfig.getSnapshotDir());
        }catch (Exception e){
            throw new RaftException(RaftError.LOAD_SNAPSHOT_ERROR, e.toString());
        }

    }

    /**
     * 因为此方法节点初始化时调用，所以不需要加锁
     *
     * 加载raft日志，应用snapshot LastIncludedIndex到commitIndex之间的日志。
     * 1. 读取日志，恢复Node中的状态变量votedFor、commitIndex、currentTerm、lastAppliedIndex（日志中的变量大于等于快照中的变量）
     * 2. 将快照中未包含的但是已经提交的日志条目应用到状态机（所以加载日志需要在加载快照之后进行）
     */
    private void loadRaftLog(){
        votedFor = raftLog.getNodePersistMetaData().getVotedFor();
        commitIndex = Math.max(commitIndex, raftLog.getNodePersistMetaData().getCommitIndex());//日志持久化的commitIndex一定大于等于快照中的commitIndex
        currentTerm = Math.max(currentTerm, raftLog.getNodePersistMetaData().getCurrentTerm());//日志持久化的currentTerm一定大于等于快照中的currentTerm

        for(long index = snapshotMetaData.getLastIncludedIndex() + 1; index <= commitIndex; index++){
            LogEntry logEntry = raftLog.read(index);
            if (logEntry.getCommand().getCommandType() == Command.CommandType.DATA)
                stateMachine.apply(logEntry);
            else if (logEntry.getCommand().getCommandType() == Command.CommandType.CONFIGURATION)
                applyConfiguration(logEntry);
            lastAppliedIndex = index;
        }
    }

    /**----------------------------------集群成员以及配置变更------------------------------**/

    /**
     * 更改日志条目中对应的配置
     * https://segmentfault.com/a/1190000022796386
     * https://www.cnblogs.com/foxmailed/p/7190642.html
     * @param logEntry
     */
    public void applyConfiguration(LogEntry logEntry) {
        Command command = logEntry.getCommand();
        raftConfig.set(command.getKey(), command.getValue());
        //更新同伴节点
        if (command.getKey().equals(RaftConfig.CLUSTER_ADDRESS_FIELD_NAME)){
            Map<Integer, String> peerAddress = raftConfig.getPeersAddress();
            //判断增加节点配置
            for(Map.Entry<Integer, String> entry : peerAddress.entrySet()){
                if (!peerMap.containsKey(entry.getKey())){
                    Peer peer = new Peer(entry.getKey(), entry.getValue(), raftLog.getLastIndex() + 1);
                    peerMap.put(entry.getKey(), peer);
                }
            }
            //判断移出节点配置
            for (Map.Entry<Integer, Peer> entry : peerMap.entrySet()){
                if (!peerAddress.containsKey(entry.getKey()))
                    peerMap.remove(entry.getKey());
            }
        }

        logger.info("set conf :{} = {}, leaderId={}", command.getKey(), command.getValue(), leaderId);
    }

    /**---------------------------------utils method-------------------------------**/


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
        logger.info("handle term lagging : myTerm {}, newTerm {}", currentTerm, newTerm);
        state = NodeState.FOLLOWER;
        currentTerm = newTerm;
        leaderId = 0;
        votedFor = 0;
        raftLog.updateNodePersistMetaData(NodePersistMetadata.builder().currentTerm(currentTerm).votedFor(votedFor).build());

        // 如果是leader的话，需要停止心跳计时器
        if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
            heartbeatScheduledFuture.cancel(true);
        }
        //开启选举定时器
        resetElectionTimer();
    }



    public long getLastLogTerm(){
        LogEntry lastLogEntry = raftLog.getLast();
        return lastLogEntry == null? snapshotMetaData.getLastIncludedTerm(): lastLogEntry.getTerm();//当生成快照后，日志为空。
    }

    public long getLastLogIndex(){
        return raftLog.getLastIndex();//日志中持久化了最大Index，可以直接获取
    }


    /**----------------------------test private method--------------------------**/

    /**
     * args[0] = configPath
     * @param args
     */
    public static void checkRecover(String[] args) {

        /**-----------------------节点首次启动--------------------------**/

        /*
        RaftConfig config = new RaftConfig();
        RaftNode node = new RaftNode(config);

        config.setSnapshotMinLogSize(20);//修改RaftHome配置

        StateMachine stateMachine = node.stateMachine;
        RaftLog raftLog = node.raftLog;
        raftLog.updateNodePersistMetaData(NodePersistMetadata.builder().votedFor(1).currentTerm(1).build());//持久化votedFor、currentTerm
        for(int i = 1; i < 100; i++){
            LogEntry logEntry = new LogEntry(0,1,new Command(i + "", i));
            raftLog.write(logEntry);
            //commitIndex = 69
            if (i < 70){
                node.commitIndex = logEntry.getIndex();
                stateMachine.apply(logEntry);
                node.lastAppliedIndex = logEntry.getIndex();
                raftLog.updateNodePersistMetaData(NodePersistMetadata.builder().commitIndex(logEntry.getIndex()).build());
            }
            //index = 50时，生成快照
            if (i == 50) {
                node.takeSnapshot();
            }
        }

        //check statemachine，应该包含Index：1 - 69的数据
        for (int i = 1; i < 100; i++){
            System.out.println(stateMachine.get(i + ""));//正确输出: index : 1 - 69 有输出， index：70 - 99 输出为null
        }

        //check raftlog
        System.out.println(raftLog.getTotalSize());//正确输出：49
        System.out.println(raftLog.getLastIndex());//正确输出：99
         */

        /**-----------------------节点故障重新启动--------------------------**/

        RaftNode node = new RaftNode(args[0]);

        StateMachine stateMachine = node.stateMachine;
        RaftLog raftLog = node.raftLog;

        //check statemachine，应该包含Index：1 - 69的数据
        for (int i = 1; i < 100; i++){
            System.out.println(stateMachine.get(i + ""));//正确输出: index : 1 - 69 有输出， index：70 - 99 输出为null
        }

        //check raftlog
        System.out.println(raftLog.getTotalSize());//正确输出：49
        System.out.println(raftLog.getLastIndex());//正确输出：99

        //check node state
        System.out.println(node.votedFor);//正确输出：1
        System.out.println(node.currentTerm);//正确输出：1
        System.out.println(node.commitIndex);//正确输出：69
        System.out.println(node.lastAppliedIndex);//正确输出：69

        //check raftConfig
        System.out.println(node.raftConfig);//正确输出：snapshotMinLogSize=20


    }

    /**
     * args[0] = configPath
     * @param args
     */
    public static void checkElection(String[] args){


        RaftConfig config = new RaftConfig();
        RaftNode node = new RaftNode(args[0]);
        node.start();

    }

    /**
     * args[0] = configPath
     * @param args
     */
    public static void checkAppendEntries(String[] args){
        RaftConfig config = new RaftConfig();
        /*
        config.setRaftHome(config.getRaftHome() + args[0]);
        config.setNodeId(Integer.parseInt(args[0]));

         */
        Map<Integer, String> clusterAddress = new HashMap<>();
        clusterAddress.put(1, "127.0.0.1:9997");
        clusterAddress.put(2, "127.0.0.1:9998");
        clusterAddress.put(3, "127.0.0.1:9999");
        config.setClusterAddress(clusterAddress);
        RaftNode node = new RaftNode(args[0]);
        node.start();
    }


    /**
     * args[0] = configPath
     * @param args
     */
    public static void checkInstallSnapshot(String[] args){

        RaftNode node = new RaftNode(args[0]);
        /*
        ThreadPoolUtils.getScheduledThreadPool().schedule(()->{
            System.out.println("------------------------------------------开始生成快照！---------------------");
            node.takeSnapshot();
        }, 20, TimeUnit.SECONDS);
         */
        node.start();

    }

    public static void main(String[] args) {
        checkInstallSnapshot(args);
    }

}
