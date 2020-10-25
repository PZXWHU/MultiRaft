package com.pzx.raft.snapshot;

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

}
