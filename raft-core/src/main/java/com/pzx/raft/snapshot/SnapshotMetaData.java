package com.pzx.raft.snapshot;

import com.pzx.raft.config.RaftConfig;
import com.pzx.raft.utils.MyFileUtils;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SnapshotMetaData implements Snapshot{

    private final static Logger logger = LoggerFactory.getLogger(SnapshotMetaData.class);

    private long lastIncludedIndex;

    private long lastIncludedTerm;

    private RaftConfig raftConfig;

    /**
     * 写时复制，不需要加锁
     * @param snapshotDir snapshot数据输出目录
     * @throws IOException
     */
    @Override
    public void writeSnapshot(String snapshotDir) throws IOException {
        String snapshotMetaFile = snapshotDir + File.separator + "metadata";
        MyFileUtils.mkDirIfNotExist(snapshotDir);
        MyFileUtils.deleteFileIfExist(snapshotMetaFile);
        MyFileUtils.createNewFile(snapshotMetaFile);
        MyFileUtils.writeObjectToFile(snapshotMetaFile, this);

    }

    /**
     * 如果还没有snapshot，则直接返回
     */
    @Override
    public void readSnapshot(String snapshotDir) throws IOException{
        String snapshotMetaFile = snapshotDir + File.separator + "metadata";
        if(!new File(snapshotMetaFile).exists()){
            logger.info("snapshotMetaFile is not exist!");
            return;
        }

        SnapshotMetaData snapshotMetaData = MyFileUtils.readObjectFromFile(snapshotMetaFile, SnapshotMetaData.class);
        this.lastIncludedIndex = snapshotMetaData.getLastIncludedIndex();
        this.lastIncludedTerm = snapshotMetaData.getLastIncludedTerm();
        this.raftConfig = snapshotMetaData.getRaftConfig();
    }
}
