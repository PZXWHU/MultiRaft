package com.pzx.raft.core.config;

import com.pzx.raft.core.RaftLogStorage;
import com.pzx.raft.core.RaftMetaStorage;
import com.pzx.raft.core.RaftStateMachine;
import lombok.*;

import java.io.File;

@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfig {

    public static final String NODE_ID_CONFIG_NAME = "serverId";
    public static final String RAFT_HOME_CONFIG_NAME = "raftHome";

    public static final String SNAPSHOT_DIR_NAME = "snapshot";
    public static final String LOG_DIR_NAME = "log";

    //本节点的id
    private long serverId;

    private long groupId;

    private RaftConfig raftConfig;

    private RaftStateMachine raftStateMachine;

    private RaftMetaStorage raftMetaStorage;

    private RaftLogStorage raftLogStorage;

    //raft工作的主目录， raft的主目录只允许根据配置文件设定，后续不能改变，否则重启之后，无法找到快照
    public String nodeHome;

    public NodeConfig(long serverId, long groupId){
        this.serverId = serverId;
        this.groupId = groupId;
    }

    public String getSnapshotDir(){
        return nodeHome + File.separator + SNAPSHOT_DIR_NAME + File.separator + groupId;
    }


}
