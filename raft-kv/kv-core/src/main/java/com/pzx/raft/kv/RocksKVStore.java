package com.pzx.raft.kv;

import com.pzx.raft.core.utils.ByteUtils;
import com.pzx.raft.kv.exception.StorageException;
import com.sun.javafx.scene.layout.region.SliceSequenceConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class RocksKVStore implements PerKVStore {

    private RocksDB db;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private Lock readLock = lock.readLock();

    private Lock writeLock = lock.writeLock();

    private String dbDirPath;

    //记录db中的最大的key，用于getApproximateKeysInRange函数和getAverageKVSize函数中，不必要精确，只需要大于等于db中的最大key即可
    private byte[] maxKey;

    private static final String MAX_VALUE = "max_value";

    private final List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();

    private ColumnFamilyHandle defaultHandle;


    static {
        RocksDB.loadLibrary();
    }

    public RocksKVStore(String dbDirPath) {
        this.dbDirPath = dbDirPath;
        openRocksDB(dbDirPath);
        initMaxValue();
    }

    private void initMaxValue(){
        try {
            maxKey = db.get(ByteUtils.stringToBytes(MAX_VALUE));
        }catch (RocksDBException e){
            log.warn(e.getMessage());
        }
        maxKey = maxKey == null ? new byte[0] : maxKey;
    }

    private void updateMaxValue(byte[] key) throws RocksDBException{
        if (ByteUtils.compare(key, maxKey) > 0){
            maxKey = key;
            db.put(ByteUtils.stringToBytes(MAX_VALUE), maxKey);
        }
    }

    @Override
    public byte[] get(byte[] key) {
        readLock.lock();
        try {
            return db.get(key);
        }catch (RocksDBException e){
            log.warn(e.getMessage());
        }finally {
            readLock.unlock();
        }
        return null;
    }

    @Override
    public boolean put(byte[] key, byte[] value) {
        writeLock.lock();
        try {
            db.put(key, value);
            updateMaxValue(key);
            return true;
        }catch (RocksDBException e){
            log.warn(e.getMessage());
        }finally {
            writeLock.unlock();
        }
        return false;
    }

    @Override
    public boolean delete(byte[] key) {
        writeLock.lock();
        try {
            db.delete(key);
            return true;
        }catch (RocksDBException e){
            log.warn(e.getMessage());
        }finally {
            writeLock.unlock();
        }
        return false;
    }

    @Override
    public List<byte[]> scan(byte[] startKey, byte[] endKey) {
        List<byte[]> res = new ArrayList<>();
        readLock.lock();
        try (final RocksIterator it = this.db.newIterator()){
            if (startKey == null) {
                it.seekToFirst();
            } else {
                it.seek(startKey);
            }
            while (it.isValid()) {
                final byte[] key = it.key();
                if (endKey != null && ByteUtils.compare(key, endKey) >= 0) {
                    break;
                }
                res.add(it.value());
                it.next();
            }
        }finally {
            readLock.unlock();
        }
        return res;
    }


    @Override
    public Lock getWriteLock() {
        return writeLock;
    }

    @Override
    public Lock getReadLock() {
        return readLock;
    }

    @Override
    public void writeSnapshot(String snapshotDirPath) throws Exception {
        writeLock.lock();
        try (final Checkpoint checkpoint = Checkpoint.create(this.db)) {
            checkpoint.createCheckpoint(snapshotDirPath);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void readSnapshot(String snapshotDirPath) throws IOException {
        File snapshotDir = new File(snapshotDirPath);
        if (!snapshotDir.exists()) return;

        readLock.lock();
        try {
            closeRocksDB();
            File dbDir = new File(dbDirPath);
            FileUtils.deleteDirectory(dbDir);
            FileUtils.copyDirectory(snapshotDir, dbDir);
            openRocksDB(dbDirPath);
        }finally {
            readLock.unlock();
        }
    }

    public void writeSstSnapshot(final String snapshotPath, final byte[] startKey, final byte[] endKey) throws IOException{
        readLock.lock();
        try {
            final String tempPath = snapshotPath + "_temp";
            File tmpFile = new File(tempPath);
            FileUtils.deleteDirectory(tmpFile);
            FileUtils.forceMkdir(tmpFile);

            final EnumMap<SstColumnFamily, File> sstFileTable = getSstFileTable(tempPath);
            createSstFiles(sstFileTable, startKey, endKey);

        } finally{
            readLock.unlock();
        }
    }

    public void readSstSnapshot(final String snapshotPath) {
        readLock.lock();
        try {
            final EnumMap<SstColumnFamily, File> sstFileTable = getSstFileTable(snapshotPath);
            ingestSstFiles(sstFileTable);
        }finally {
            readLock.unlock();
        }
    }

    private EnumMap<SstColumnFamily, File> getSstFileTable(final String path) {
        final EnumMap<SstColumnFamily, File> sstFileTable = new EnumMap<>(SstColumnFamily.class);
        sstFileTable.put(SstColumnFamily.DEFAULT, Paths.get(path, "default.sst").toFile());
        sstFileTable.put(SstColumnFamily.SEQUENCE, Paths.get(path, "sequence.sst").toFile());
        sstFileTable.put(SstColumnFamily.LOCKING, Paths.get(path, "locking.sst").toFile());
        sstFileTable.put(SstColumnFamily.FENCING, Paths.get(path, "fencing.sst").toFile());
        return sstFileTable;
    }

    private void createSstFiles(final EnumMap<SstColumnFamily, File> sstFileTable, final byte[] startKey, final byte[] endKey) {
        Snapshot snapshot = null;
        final CompletableFuture<Void> sstFuture = new CompletableFuture<>();
        readLock.lock();
        try(final ReadOptions readOptions = new ReadOptions();
            final EnvOptions envOptions = new EnvOptions();
            final Options options = new Options().setMergeOperator(new StringAppendOperator())) {

            snapshot = this.db.getSnapshot();
            readOptions.setSnapshot(snapshot);
            for (final Map.Entry<SstColumnFamily, File> entry : sstFileTable.entrySet()) {
                final SstColumnFamily sstColumnFamily = entry.getKey();
                final File sstFile = entry.getValue();
                final ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(sstColumnFamily);
                try (final RocksIterator it = this.db.newIterator(columnFamilyHandle, readOptions);
                     final SstFileWriter sstFileWriter = new SstFileWriter(envOptions, options)) {
                    if (startKey == null) {
                        it.seekToFirst();
                    } else {
                        it.seek(startKey);
                    }
                    sstFileWriter.open(sstFile.getAbsolutePath());
                    long count = 0;
                    for (;;) {
                        if (!it.isValid()) {
                            break;
                        }
                        final byte[] key = it.key();
                        if (endKey != null && ByteUtils.compare(key, endKey) >= 0) {
                            break;
                        }
                        sstFileWriter.put(key, it.value());
                        ++count;
                        it.next();
                    }
                    if (count == 0) {
                        sstFileWriter.close();
                    } else {
                        sstFileWriter.finish();
                    }
                    log.info("Finish sst file {} with {} keys.", sstFile, count);
                } catch (final RocksDBException e) {
                    throw new StorageException("Fail to create sst file at path: " + sstFile, e);
                }
            }
        }finally {
            // Nothing to release, rocksDB never own the pointer for a snapshot.
            snapshot.close();
            // The pointer to the snapshot is released by the database instance.
            this.db.releaseSnapshot(snapshot);
            readLock.unlock();
        }
    }

    private ColumnFamilyHandle findColumnFamilyHandle(final SstColumnFamily sstColumnFamily) {
        switch (sstColumnFamily) {
            case DEFAULT:
                return this.defaultHandle;
            default:
                throw new IllegalArgumentException("illegal sstColumnFamily: " + sstColumnFamily.name());
        }
    }

    private void ingestSstFiles(final EnumMap<SstColumnFamily, File> sstFileTable) {
        readLock.lock();
        try {
            for (final Map.Entry<SstColumnFamily, File> entry : sstFileTable.entrySet()) {
                final SstColumnFamily sstColumnFamily = entry.getKey();
                final File sstFile = entry.getValue();
                final ColumnFamilyHandle columnFamilyHandle = findColumnFamilyHandle(sstColumnFamily);
                try (final IngestExternalFileOptions ingestOptions = new IngestExternalFileOptions()) {
                    if (!sstFile.exists() || FileUtils.sizeOf(sstFile) == 0L) {
                        return;
                    }
                    final String filePath = sstFile.getAbsolutePath();
                    log.info("Start ingest sst file {}.", filePath);
                    this.db.ingestExternalFile(columnFamilyHandle, Collections.singletonList(filePath), ingestOptions);
                } catch (final RocksDBException e) {
                    throw new StorageException("Fail to ingest sst file at path: " + sstFile, e);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getApproximateKeysInRange(final byte[] startKey, final byte[] endKey) {
        // TODO This is a sad code, the performance is too damn bad

        final Snapshot snapshot = this.db.getSnapshot();
        try (final ReadOptions readOptions = new ReadOptions()) {
            readOptions.setSnapshot(snapshot);
            try (final RocksIterator it = this.db.newIterator(readOptions)) {
                if (startKey == null) {
                    it.seekToFirst();
                } else {
                    it.seek(startKey);
                }
                long approximateKeys = 0;
                for (;;) {
                    // The accuracy is 100, don't ask more
                    for (int i = 0; i < 100; i++) {
                        if (!it.isValid()) {
                            return approximateKeys;
                        }
                        it.next();
                        ++approximateKeys;
                    }
                    if (endKey != null && ByteUtils.compare(it.key(), endKey) >= 0) {
                        return approximateKeys;
                    }
                }
            }
        } finally {
            // Nothing to release, rocksDB never own the pointer for a snapshot.
            snapshot.close();
            // The pointer to the snapshot is released by the database instance.
            this.db.releaseSnapshot(snapshot);
        }
    }
/*
    private double getAverageKVSize() throws RocksDBException{
        Slice slice = new Slice(ByteUtils.BYTE_ARRAY_MIN_VALUE);
        Slice slice1 = new Slice(maxKey);
        Range range = new Range(slice, slice1);
        long approximateSize = db.getApproximateSizes(Collections.singletonList(range),
                SizeApproximationFlag.INCLUDE_FILES, SizeApproximationFlag.INCLUDE_MEMTABLES)[0];
        long kvNum = Long.valueOf(db.getProperty("rocksdb.estimate-num-keys"));
        return approximateSize / (double)kvNum;
    }

    @Override
    *//**
     * 首先获取整个db的估计kv数量以及所占磁盘大小
     * 然后根据平均kv大小和范围内的所有kv总大小，计算出范围内的kv数量
     *//*
    public long getApproximateKeysInRange(byte[] startKey, byte[] endKey) {
        Slice start = new Slice(startKey);
        if (endKey == ByteUtils.BYTE_ARRAY_MAX_VALUE) endKey = maxKey;
        Slice end = new Slice(endKey);
        Range range = new Range(start, end);
        long approximateSize = db.getApproximateSizes(Collections.singletonList(range),
                SizeApproximationFlag.INCLUDE_FILES, SizeApproximationFlag.INCLUDE_MEMTABLES)[0];
        long approximateNum = - 1;
        try {
            approximateNum = (long)(approximateSize / getAverageKVSize());
        }catch (RocksDBException e){
            log.warn(e.getMessage());
        }
        return approximateNum;
    }*/


    @Override
    public byte[] jumpOver(final byte[] startKey, final long distance) {
        readLock.lock();
        final Snapshot snapshot = this.db.getSnapshot();
        try (final ReadOptions readOptions = new ReadOptions()) {
            readOptions.setSnapshot(snapshot);
            try (final RocksIterator it = this.db.newIterator(readOptions)) {
                if (startKey == null) {
                    it.seekToFirst();
                } else {
                    it.seek(startKey);
                }
                long approximateKeys = 0;
                for (;;) {
                    byte[] lastKey = null;
                    if (it.isValid()) {
                        lastKey = it.key();
                    }
                    // The accuracy is 100, don't ask more
                    for (int i = 0; i < 100; i++) {
                        if (!it.isValid()) {
                            return lastKey;
                        }
                        it.next();
                        if (++approximateKeys >= distance) {
                            return it.key();
                        }
                    }
                }
            }
        } finally {
            // Nothing to release, rocksDB never own the pointer for a snapshot.
            snapshot.close();
            // The pointer to the snapshot is released by the database instance.
            this.db.releaseSnapshot(snapshot);
            readLock.unlock();
        }
    }


    private void openRocksDB(String dirPath){
        DBOptions dbOptions = new DBOptions();
        dbOptions.setCreateIfMissing(true);
        try {
            this.cfDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
            final List<ColumnFamilyHandle> cfHandles = new ArrayList<>();
            db = RocksDB.open(dbOptions, dirPath,  this.cfDescriptors, cfHandles);
            this.defaultHandle = cfHandles.get(0);
        }catch (RocksDBException e){
            throw new StorageException("failed to open RocksDB : " + dirPath , e);
        }
    }


    private void closeRocksDB() {
        if (this.db != null) {
            this.db.close();
            this.db = null;
        }
    }

    public enum SstColumnFamily {

        DEFAULT(0), SEQUENCE(1), LOCKING(2), FENCING(3);

        private final int value;

        SstColumnFamily(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
