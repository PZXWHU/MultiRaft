package com.pzx.raft.node;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
@Builder
@ToString
public class NodePersistMetadata {

    public static final byte[] CURRENT_TERM_KEY = "currentTerm".getBytes();
    public static final byte[] VOTED_FOR_KEY = "votedFor".getBytes();
    public static final byte[] COMMIT_INDEX_KEY = "commitIndex".getBytes();
    public static final Lock LOCK = new ReentrantLock();

    private long currentTerm;

    private int votedFor;

    private long commitIndex;

}
