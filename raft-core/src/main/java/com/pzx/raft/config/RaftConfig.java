package com.pzx.raft.config;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author PZX
 */
@Getter
@Setter
@ToString
public class RaftConfig {

    private final static Logger logger = LoggerFactory.getLogger(RaftConfig.class);

    //本节点的id
    public int nodeId;

    //解析之后的集群地址
    public Map<Integer, String> clusterAddress;

    //raft工作的主目录， raft的主目录只允许根据配置文件设定，后续不能改变，否则重启之后，无法找到快照
    public String raftHome = ".";


    /*-----------------------------Constant----------------------------------*/

    // A follower would become a candidate if it doesn't receive any message
    // from the leader in electionTimeoutMs milliseconds
    public int electionTimeoutMilliseconds = 5000;

    // A leader sends RPCs at least this often, even if there is no data to send
    public int heartbeatPeriodMilliseconds = 500;

    // snapshot定时器执行间隔
    public int snapshotPeriodSeconds = 3600;

    // log entry大小达到snapshotMinLogSize，才做snapshot
    public int snapshotMinLogSize = 100 * 1024 * 1024;

    public int maxSnapshotBytesPerRequest = 500 * 1024; // 500k

    public int maxLogEntriesPerRequest = 5000;

    // 单个segment文件大小，默认100m
    public int maxSegmentFileSize = 100 * 1000 * 1000;

    // follower与leader差距在catchupMargin，才可以参与选举和提供服务
    public long catchupMargin = 500;

    // replicate最大等待超时时间，单位ms
    public long maxAwaitTimeout = 1000;

    // 与其他节点进行同步、选主等操作的线程池大小
    public int raftConsensusThreadNum = 20;

    // 是否异步写数据；true表示主节点保存后就返回，然后异步同步给从节点；
    // false表示主节点同步给大多数从节点后才返回。
    public boolean asyncWrite = false;

    public int getClusterSize(){
        return clusterAddress.size();
    }

    public String getSelfAddress(){
        return clusterAddress.get(nodeId);
    }

    public List<String> getPeersAddress(){
        return clusterAddress.entrySet().stream()
                .filter(entry -> entry.getKey() == nodeId)
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
    }

    public RaftConfig copy(){
        Gson gson = new Gson();
        String jsonString = gson.toJson(this);
        return gson.fromJson(jsonString, RaftConfig.class);
    }

    public String getSnapshotDir(){
        return this.getRaftHome() + File.separator + "snapshot";
    }

    public String getLogDir(){ return this.getRaftHome() + File.separator + "log"; }


}
