package com.pzx.raft.node;

import com.pzx.raft.config.RaftConfig;
import com.pzx.raft.log.Command;
import com.pzx.raft.log.LogEntry;
import com.pzx.raft.exception.RaftError;
import com.pzx.raft.exception.RaftException;
import com.pzx.raft.log.RocksDBRaftLog;
import com.pzx.raft.statemachine.MemoryMapStateMachine;
import com.pzx.raft.statemachine.StateMachine;
import com.pzx.raft.log.RaftLog;
import com.pzx.raft.snapshot.SnapshotMetaData;
import com.pzx.raft.utils.MyFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class RaftNode {

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
    private long votedFor = 0;

    //已知已提交的最高的日志条目的索引（初始值为0，单调递增）
    private long commitIndex = 0;

    //已经被应用到状态机的最高的日志条目的索引（初始值为0，单调递增）
    private long  lastAppliedIndex = 0;

    //表示是否正在安装snapshot，leader向follower安装，leader和follower同时处于installSnapshot状态
    private AtomicBoolean isInstallSnapshot = new AtomicBoolean(false);

    //表示节点自己是否在对状态机做snapshot
    private AtomicBoolean isTakeSnapshot = new AtomicBoolean(false);

    private Lock lock = new ReentrantLock();

    public RaftNode(RaftConfig raftConfig){
        this.raftConfig = raftConfig;//读取默认配置
        MyFileUtils.mkDirIfNotExist(raftConfig.getRaftHome());//创建Raft主目录
        this.stateMachine = new MemoryMapStateMachine();
        this.raftLog = new RocksDBRaftLog(raftConfig.getLogDir());

        recover();//恢复上一次宕机的状态


    }



    /**
     * 每个服务器独立的创建快照，快照内容：
     * 1.已经被提交的日志，即状态机的状态
     * 2.元数据：last included index（状态机最后应用的日志的索引）、last included term（状态机最后应用的日志的任期）、raft配置
     */
    private void takeSnapshot(){
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
            long lastIncludedIndex, lastIncludedTerm;
            StateMachine copyStateMachine;
            RaftConfig copyConfig;

            lock.lock();
            try {
                lastIncludedIndex = lastAppliedIndex;
                lastIncludedTerm = raftLog.read(lastIncludedIndex).getTerm();
                copyStateMachine = stateMachine.copy();
                copyConfig = raftConfig.copy();
            }finally {
                lock.unlock();
            }

            logger.info("start taking snapshot");
            boolean success = false;
            String snapshotDir = raftConfig.getSnapshotDir();
            String tmpSnapshotDir = snapshotDir +  ".tmp";

            try {
                //3.先将snapshot写入临时文件夹，避免take snapshot失败，覆盖了以前的snapshot
                //3.1 take metadata snapshot
                SnapshotMetaData snapshotMetaData = SnapshotMetaData.builder()
                        .lastIncludedIndex(lastIncludedIndex)
                        .lastIncludedTerm(lastIncludedTerm)
                        .raftConfig(copyConfig)
                        .build();

                snapshotMetaData.writeSnapshot(tmpSnapshotDir);
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

            //5. 如果take snapshot成功，则丢弃旧的日志条目
            if (success)
                raftLog.removeToEndIndex(lastIncludedIndex);
        }finally {
            isTakeSnapshot.compareAndSet(true, false);
        }
    }

    /**
     * 加载快照和日志，恢复上一次宕机的状态
     */
    private void recover(){
        loadRaftLog(loadSnapshot());
    }

    /**
     * 因为此方法节点初始化时调用，所以不需要加锁
     * 节点重启时读取Snapshot
     *
     * 1.读取Snapshot，恢复Node中的状态变量的commitIndex、currentTerm、lastAppliedIndex以及配置raftConfig
     * 2.状态机读取Snapshot
     * @throws Exception
     * @return LastIncludedIndex
     */
    private long loadSnapshot()  {
        try {
            SnapshotMetaData snapshotMetaData = new SnapshotMetaData();
            snapshotMetaData.readSnapshot(raftConfig.getSnapshotDir());
            commitIndex = snapshotMetaData.getLastIncludedIndex();
            currentTerm = snapshotMetaData.getLastIncludedTerm();
            lastAppliedIndex = commitIndex;
            raftConfig = snapshotMetaData.getRaftConfig() == null ? raftConfig : snapshotMetaData.getRaftConfig();
            stateMachine.readSnapshot(raftConfig.getSnapshotDir());

            return snapshotMetaData.getLastIncludedIndex();
        }catch (Exception e){
            throw new RaftException(RaftError.LOAD_SNAPSHOT_ERROR, e.toString());
        }

    }

    /**
     * 因为此方法节点初始化时调用，所以不需要加锁
     *
     * 加载raft日志，应用snapshot LastIncludedIndex到commitIndex之间的日志。
     * 1. 读取日志，恢复Node中的状态变量commitIndex、currentTerm、lastAppliedIndex（日志中的变量大于等于快照中的变量）
     * 2. 将快照中未包含的但是已经提交的日志条目应用到状态机（所以加载日志需要在加载快照之后进行）
     */
    private void loadRaftLog(long snapshotLastIncludedIndex){

        votedFor = raftLog.getNodePersistMetaData().getVotedFor();
        commitIndex = raftLog.getNodePersistMetaData().getCommitIndex();//日志持久化的commitIndex一定大于等于快照中的commitIndex
        currentTerm = raftLog.getNodePersistMetaData().getCurrentTerm();//日志持久化的currentTerm一定大于等于快照中的currentTerm
        lastAppliedIndex = commitIndex;
        for(long index = snapshotLastIncludedIndex + 1; index <= lastAppliedIndex; index++){
            LogEntry logEntry = raftLog.read(index);
            stateMachine.apply(logEntry);
        }

    }


    /**----------------------------test private method--------------------------**/

    public static void checkRecover(String[] args) {

        /**-----------------------节点首次启动--------------------------**/

        /*
        RaftConfig config = new RaftConfig();
        RaftNode node = new RaftNode(config);

        config.setSnapshotMinLogSize(20);//修改RaftHome配置

        StateMachine stateMachine = node.stateMachine;
        RaftLog raftLog = node.raftLog;
        raftLog.updateNodePersistMetaData(NodePersistMetaData.builder().votedFor(1).currentTerm(1).build());//持久化votedFor、currentTerm
        for(int i = 1; i < 100; i++){
            LogEntry logEntry = new LogEntry(0,1,new Command(i + "", i));
            raftLog.write(logEntry);
            //commitIndex = 69
            if (i < 70){
                node.commitIndex = logEntry.getIndex();
                stateMachine.apply(logEntry);
                node.lastAppliedIndex = logEntry.getIndex();
                raftLog.updateNodePersistMetaData(NodePersistMetaData.builder().commitIndex(logEntry.getIndex()).build());
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

        RaftConfig config = new RaftConfig();
        RaftNode node = new RaftNode(config);

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


    public static void main(String[] args) {

    }

}
