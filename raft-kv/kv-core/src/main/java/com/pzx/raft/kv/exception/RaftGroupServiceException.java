package com.pzx.raft.kv.exception;

public class RaftGroupServiceException extends RuntimeException {

    public RaftGroupServiceException() {
    }

    public RaftGroupServiceException(String message) {
        super(message);
    }

    public RaftGroupServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RaftGroupServiceException(Throwable cause) {
        super(cause);
    }

    public RaftGroupServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
