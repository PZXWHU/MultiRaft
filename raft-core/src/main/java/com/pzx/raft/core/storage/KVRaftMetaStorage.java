package com.pzx.raft.core.storage;

import com.pzx.raft.core.RaftMetaStorage;
import com.pzx.raft.kv.KVPrefixAdapter;
import com.pzx.raft.kv.PerKVStore;
import com.pzx.raft.core.utils.ByteUtils;


public class KVRaftMetaStorage implements RaftMetaStorage {

    private static final String META_PREFIX = "meta_";

    private static final String CURRENT_TERM = "currentTerm";

    private static final String VOTED_FOR = "votedFor";

    private static final String SNAPSHOT_LAST_INCLUDE_INDEX = "snapshotLastIncludedIndex";

    private static final String SNAPSHOT_LAST_INCLUDE_TERM = "snapshotLastIncludedTerm";

    private static final String LAST_APPLIED_INDEX = "lastAppliedIndex";

    private long currentTerm = 0;

    private long votedFor = 0;

    private long snapshotLastIncludedIndex = 0;

    private long snapshotLastIncludedTerm = 0;

    private long lastAppliedIndex = 0;

    private KVPrefixAdapter kvPrefixAdapter;

    private String prefix;

    public KVRaftMetaStorage(long groupId, PerKVStore kvStore){
        this.prefix = META_PREFIX + groupId + "_";
        this.kvPrefixAdapter = new KVPrefixAdapter(kvStore, prefix);
    }

    @Override
    public boolean setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
        return kvPrefixAdapter.put(ByteUtils.stringToBytes(CURRENT_TERM), ByteUtils.longToBytes(currentTerm));
    }
    @Override
    public boolean setVotedFor(long votedFor) {
        this.votedFor = votedFor;
        return kvPrefixAdapter.put(ByteUtils.stringToBytes(VOTED_FOR), ByteUtils.longToBytes(votedFor));
    }

    @Override
    public boolean setSnapshotLastIncludedIndex(long snapshotLastIncludedIndex) {
        this.snapshotLastIncludedIndex = snapshotLastIncludedIndex;
        return kvPrefixAdapter.put(ByteUtils.stringToBytes(SNAPSHOT_LAST_INCLUDE_INDEX), ByteUtils.longToBytes(snapshotLastIncludedIndex));
    }
    @Override
    public boolean setSnapshotLastIncludedTerm(long snapshotLastIncludedTerm) {
        this.snapshotLastIncludedTerm = snapshotLastIncludedTerm;
        return kvPrefixAdapter.put(ByteUtils.stringToBytes(SNAPSHOT_LAST_INCLUDE_TERM), ByteUtils.longToBytes(snapshotLastIncludedTerm));
    }

    @Override
    public boolean setLastAppliedIndex(long lastAppliedIndex) {
        this.lastAppliedIndex = lastAppliedIndex;
        return kvPrefixAdapter.put(ByteUtils.stringToBytes(LAST_APPLIED_INDEX), ByteUtils.longToBytes(lastAppliedIndex));
    }

    @Override
    public long getCurrentTerm() {
        if (currentTerm == 0){
            byte[] currentTermBytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(CURRENT_TERM));
            if (currentTermBytes != null)
                currentTerm = ByteUtils.bytesToLong(currentTermBytes);
        }
        return currentTerm;
    }
    @Override
    public long getVotedFor() {
        if (votedFor == 0){
            byte[] voteForBytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(VOTED_FOR));
            if (voteForBytes != null)
                votedFor = ByteUtils.bytesToLong(voteForBytes);
        }
        return votedFor;
    }
    @Override
    public long getSnapshotLastIncludedIndex() {
        if (snapshotLastIncludedIndex == 0){
            byte[] snapshotLastIncludedIndexBytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(SNAPSHOT_LAST_INCLUDE_INDEX));
            if (snapshotLastIncludedIndexBytes != null)
                snapshotLastIncludedIndex = ByteUtils.bytesToLong(snapshotLastIncludedIndexBytes);
        }
        return snapshotLastIncludedIndex;
    }
    @Override
    public long getSnapshotLastIncludedTerm() {
        if (snapshotLastIncludedTerm == 0){
            byte[] snapshotLastIncludedTermBytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(SNAPSHOT_LAST_INCLUDE_TERM));
            if (snapshotLastIncludedTermBytes != null)
                snapshotLastIncludedTerm = ByteUtils.bytesToLong(snapshotLastIncludedTermBytes);
        }
        return snapshotLastIncludedTerm;
    }

    @Override
    public long getLastAppliedIndex() {
        if(lastAppliedIndex == 0){
            byte[] lastAppliedIndexBytes = kvPrefixAdapter.get(ByteUtils.stringToBytes(LAST_APPLIED_INDEX));
            if (lastAppliedIndexBytes != null)
                lastAppliedIndex = ByteUtils.bytesToLong(lastAppliedIndexBytes);
        }
        return lastAppliedIndex;
    }

}
