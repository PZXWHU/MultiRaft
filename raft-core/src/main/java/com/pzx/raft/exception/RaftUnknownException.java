package com.pzx.raft.exception;

public class RaftUnknownException extends RuntimeException {

    public RaftUnknownException() {
    }

    public RaftUnknownException(String message) {
        super(message);
    }

    public RaftUnknownException(String message, Throwable cause) {
        super(message, cause);
    }

    public RaftUnknownException(Throwable cause) {
        super(cause);
    }

    public RaftUnknownException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
