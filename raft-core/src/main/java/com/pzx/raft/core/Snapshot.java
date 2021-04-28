package com.pzx.raft.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public interface Snapshot {

    /**
     * 对状态机中数据进行snapshot，每个节点本地定时调用
     * @param snapshotDirPath snapshot数据输出目录
     */
    void writeSnapshot(String snapshotDirPath) throws Exception;

    /**
     * 读取snapshot到状态机，节点启动时调用
     * @param snapshotDirPath snapshot数据目录
     */
    void readSnapshot(String snapshotDirPath)throws Exception;


}
