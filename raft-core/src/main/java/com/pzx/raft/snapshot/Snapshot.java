package com.pzx.raft.snapshot;

import java.util.Map;

public interface Snapshot {

    /**
     * 对状态机中数据进行snapshot，每个节点本地定时调用
     * @param snapshotDir snapshot数据输出目录
     */
    void writeSnapshot(String snapshotDir) throws Exception;

    /**
     * 读取snapshot到状态机，节点启动时调用
     * @param snapshotDir snapshot数据目录
     */
    void readSnapshot(String snapshotDir)throws Exception;

    /**
     * 获取所有快照文件数据
     * @param snapshotDir
     * @return key：快照文件名称  value：快照文件数据
     * @throws Exception
     */
    Map<String, byte[]> getSnapshotFileData(String snapshotDir)throws Exception;

}
