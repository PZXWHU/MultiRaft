package com.pzx.raft.config;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author PZX
 */
@Getter
@Setter
@ToString
public class RaftConfig {

    private final static Logger logger = LoggerFactory.getLogger(RaftConfig.class);

    private final static Class<RaftConfig> clazz = RaftConfig.class;

    public final static String CLUSTER_ADDRESS_FIELD_NAME = "clusterAddress";


    //解析之后的集群地址
    public Map<Integer, String> clusterAddress;

    /*-----------------------------Constant----------------------------------*/

    // A follower would become a candidate if it doesn't receive any message
    // from the leader in electionTimeoutMs milliseconds
    public int electionTimeoutMilliseconds = 3000;

    // A leader sends RPCs at least this often, even if there is no data to send
    public int heartbeatPeriodMilliseconds = 400;

    // snapshot定时器执行间隔
    public int snapshotPeriodSeconds = 3600;

    // log entry大小达到snapshotMinLogSize，才做snapshot
    public int snapshotMinLogSize = 100 * 1024 * 1024;

    public int maxLogEntriesPerRequest = 50;

    // replicate最大等待超时时间，单位ms
    public long maxAwaitTimeout = 2000;

    // 与其他节点进行同步、选主等操作的线程池大小
    public int raftConsensusThreadNum = 20;

    // 是否异步写数据；true表示主节点保存后就返回，然后异步同步给从节点；
    // false表示主节点同步给大多数从节点后才返回。
    public boolean asyncWrite = false;

    public void set(String fieldName, Object value){
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        }catch (NoSuchFieldException e){
            logger.error("raftConfig 设置错误，没有{}此属性：{}", fieldName, e);
        }catch (IllegalAccessException e){
            logger.error("raftConfig 设置错误，访问{}此属性异常：{}", fieldName, e);
        }

    }

    public int getClusterSize(){
        return clusterAddress.size();
    }

    public String getSelfAddress(){
        return clusterAddress.get(NodeConfig.nodeId);
    }

    public Map<Integer, String> getPeersAddress(){
        Map<Integer, String> peersAddress = new HashMap<>(clusterAddress);
        peersAddress.remove(NodeConfig.nodeId);
        return peersAddress;
    }

    public RaftConfig copy(){
        String jsonString = JSONObject.toJSONString(this);
        RaftConfig copyConfig = JSONObject.parseObject(jsonString, RaftConfig.class);
        return copyConfig;
    }




}
