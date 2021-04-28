package com.pzx.raft.core.config;

import com.pzx.raft.core.Snapshot;
import com.pzx.raft.core.utils.MyFileUtils;
import lombok.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author PZX
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class RaftConfig implements Snapshot {

    private final static Logger logger = LoggerFactory.getLogger(RaftConfig.class);

    private final static Class<RaftConfig> clazz = RaftConfig.class;

    public final static String RAFT_GROUP_ADDRESS_Field = "raftGroupAddress";

    private final static String SNAPSHOT_RAFT_CONFIG_FILENAME = "raftConfig";

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

    public int maxSnapshotBytesPerRequest = 100 * 1024 * 1024;

    // replicate最大等待超时时间，单位ms
    public long maxAwaitTimeout = 2000;

    // 与其他节点进行同步、选主等操作的线程池大小
    public int raftConsensusThreadNum = 10;

    // 是否异步写数据；true表示主节点保存后就返回，然后异步同步给从节点；
    // false表示主节点同步给大多数从节点后才返回。
    public boolean asyncWrite = false;

    //raft复制组的地址
    public String raftGroupAddress;

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

    public Object get(String fieldName){
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(this);
        }catch (NoSuchFieldException e){
            logger.error("raftConfig 设置错误，没有{}此属性：{}", fieldName, e);
        }catch (IllegalAccessException e){
            logger.error("raftConfig 设置错误，访问{}此属性异常：{}", fieldName, e);
        }
        return null;

    }

/*    public int getClusterSize(){
        return clusterAddress.size();
    }

    public String getServerAddress(){
        return clusterAddress.get(NodeConfig.serverId);
    }
    */

    public static Map<Long, String> parseRaftGroupAddress(String raftGroupAddress){
        Map<Long, String> raftGroupAddressMap = new HashMap<>();
        for(String server : raftGroupAddress.split(",")){
            String[] arr = server.split("-");
            raftGroupAddressMap.put(Long.valueOf(arr[0]), arr[1]);
        }
        return raftGroupAddressMap;
    }

    public static String unParseRaftGroupAddress(Map<Long, String> raftGroupAddressMap){
        StringJoiner stringJoiner = new StringJoiner(",");
        for (Map.Entry<Long, String> entry : raftGroupAddressMap.entrySet()){
            stringJoiner.add(entry.getKey() + "-" + entry.getValue());
        }
        return stringJoiner.toString();
    }

    public int getRaftGroupSize(){
        return raftGroupAddress.split(",").length;
    }


    @Override
    public void writeSnapshot(String snapshotDirPath) throws IOException {
        String snapshotFilePath = snapshotDirPath + File.separator + SNAPSHOT_RAFT_CONFIG_FILENAME;
        File snapshotDir = new File(snapshotDirPath);
        File snapshotFile = new File(snapshotFilePath);

        FileUtils.forceMkdir(snapshotDir);
        FileUtils.forceDeleteOnExit(snapshotFile);
        snapshotFile.createNewFile();
        MyFileUtils.writeObjectToFile(snapshotFilePath, this);
    }

    @Override
    public void readSnapshot(String snapshotDirPath) throws IOException {
        String snapshotFile = snapshotDirPath + File.separator + SNAPSHOT_RAFT_CONFIG_FILENAME;
        if(!(new File(snapshotDirPath).exists())) return;
        RaftConfig raftConfig = MyFileUtils.readObjectFromFile(snapshotFile, RaftConfig.class);
        for(Field field : clazz.getFields()){
            if (!Modifier.isFinal(field.getModifiers()))
                set(field.getName(), raftConfig.get(field.getName()));
        }
    }


}
