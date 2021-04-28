package com.pzx.raft.core;



public interface RaftMetaStorage {

    boolean setCurrentTerm(long currentTerm);

    boolean setVotedFor(long votedFor);

    boolean setSnapshotLastIncludedIndex(long snapshotLastIncludedIndex);

    boolean setSnapshotLastIncludedTerm(long snapshotLastIncludedTerm);

    boolean setLastAppliedIndex(long lastAppliedIndex);

    long getCurrentTerm();

    long getVotedFor();

    long getSnapshotLastIncludedIndex();

    long getSnapshotLastIncludedTerm();

    long getLastAppliedIndex();

}
