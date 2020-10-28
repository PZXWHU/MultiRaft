package com.pzx.raft.snapshot;

import com.google.common.collect.Maps;
import com.pzx.raft.config.RaftConfig;
import com.pzx.raft.utils.MyFileUtils;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SnapshotMetaData implements Snapshot{

    private final static Logger logger = LoggerFactory.getLogger(SnapshotMetaData.class);

    private final static String SNAPSHOT_METADATA_FILENAME = "metadata";

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
        String snapshotMetaFile = snapshotDir + File.separator + SNAPSHOT_METADATA_FILENAME;
        MyFileUtils.mkDirIfNotExist(snapshotDir);
        MyFileUtils.deleteFileIfExist(snapshotMetaFile);
        MyFileUtils.createFileIfNotExist(snapshotMetaFile);
        MyFileUtils.writeObjectToFile(snapshotMetaFile, this);

    }

    /**
     * 如果还没有snapshot，则直接返回
     */
    @Override
    public void readSnapshot(String snapshotDir) throws IOException{
        String snapshotMetaFile = snapshotDir + File.separator + SNAPSHOT_METADATA_FILENAME;
        if(!new File(snapshotMetaFile).exists()){
            logger.info("snapshotMetaFile is not exist!");
            return;
        }

        SnapshotMetaData snapshotMetaData = MyFileUtils.readObjectFromFile(snapshotMetaFile, SnapshotMetaData.class);
        this.lastIncludedIndex = snapshotMetaData.getLastIncludedIndex();
        this.lastIncludedTerm = snapshotMetaData.getLastIncludedTerm();
        this.raftConfig = snapshotMetaData.getRaftConfig();
    }

    @Override
    public Map<String, byte[]> getSnapshotFileData(String snapshotDir) throws Exception {
        String snapshotMetaFile = snapshotDir + File.separator + SNAPSHOT_METADATA_FILENAME;
        Path path = Paths.get(snapshotMetaFile);
        if (!Files.exists(path))
            return null;
        Map<String, byte[]> snapshotData = new HashMap<>(1);
        snapshotData.put(SNAPSHOT_METADATA_FILENAME, Files.readAllBytes(path));
        return snapshotData;
    }
}
