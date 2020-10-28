package com.pzx.raft.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;

@Setter
@Getter
@ToString
public class NodeConfig {

    public static final String NODE_ID_CONFIG_NAME = "nodeId";
    public static final String RAFT_HOME_CONFIG_NAME = "raftHome";

    //本节点的id
    public static int nodeId = -1;

    //raft工作的主目录， raft的主目录只允许根据配置文件设定，后续不能改变，否则重启之后，无法找到快照
    public static String raftHome = "." + File.separator + "raftHome";


    public static String getSnapshotDir(){
        return raftHome + File.separator + "snapshot";
    }

    public static String getLogDir(){ return raftHome + File.separator + "log"; }

}
