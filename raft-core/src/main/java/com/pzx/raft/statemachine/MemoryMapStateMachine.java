package com.pzx.raft.statemachine;

import com.pzx.raft.log.Command;
import com.pzx.raft.log.LogEntry;
import com.pzx.raft.utils.MyFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryMapStateMachine implements StateMachine {

    private static final Logger logger = LoggerFactory.getLogger(MemoryMapStateMachine.class);
    private ConcurrentHashMap<String, Object> stateMachine;

    public MemoryMapStateMachine(){
        stateMachine = new ConcurrentHashMap<>();
    }

    private MemoryMapStateMachine(Map<String, Object> stateMachine){
        this.stateMachine = new ConcurrentHashMap<>(stateMachine);
    }

    /**
     * 使用ConcurrentHashMap，不需要加锁
     * @param logEntry 日志中的数据.
     */
    @Override
    public void apply(LogEntry logEntry) {
        Command command = logEntry.getCommand();
        stateMachine.put(command.getKey(), command.getValue());
    }

    @Override
    public StateMachine copy() {
        return new MemoryMapStateMachine(stateMachine);
    }

    /**
     * 因为使用写时复制，不需要加锁
     * @param snapshotDir snapshot数据输出目录
     * @throws IOException
     */
    @Override
    public void writeSnapshot(String snapshotDir) throws IOException {
        String snapshotDataFile = snapshotDir + File.separator + "data";
        MyFileUtils.mkDirIfNotExist(snapshotDir);
        MyFileUtils.deleteFileIfExist(snapshotDataFile);
        MyFileUtils.createNewFile(snapshotDataFile);
        MyFileUtils.writeObjectToFile(snapshotDataFile, this);
    }

    /**
     * Node初始化使用，不需要加锁
     * @param snapshotDir snapshot数据目录
     * @throws IOException
     */
    @Override
    public void readSnapshot(String snapshotDir) throws IOException {
        String snapshotDataFile = snapshotDir + File.separator + "data";
        if(!new File(snapshotDataFile).exists()){
            logger.info("snapshotDataFile is not exist!");
            return;
        }
        this.stateMachine = MyFileUtils.readObjectFromFile(snapshotDataFile, MemoryMapStateMachine.class).stateMachine;

    }

    @Override
    public Object get(String key) {
        return stateMachine.get(key);
    }

    @Override
    public Object set(String key, Object value) {
        return stateMachine.put(key, value);
    }

    @Override
    public Object delete(String key) {
        return stateMachine.remove(key);
    }
}
