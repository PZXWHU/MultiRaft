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

    default byte[] getUserData(byte[] key){throw new  UnsupportedOperationException();}

    default void setUserData(byte[] key, byte[] value) {throw new UnsupportedOperationException();}

}
