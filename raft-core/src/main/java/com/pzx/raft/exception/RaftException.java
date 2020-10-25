package com.pzx.raft.exception;

import io.protostuff.runtime.RuntimeSchema;

public class RaftException extends RuntimeException {

    public RaftException(RaftError e){
        super(e.getMessage());
    }

    public RaftException(RaftError e, String detail){
        super(e.getMessage() + " : " + detail);
    }

    public RaftException(String message) {
        super(message);
    }

    public RaftException(String message, Throwable cause) {
        super(message, cause);
    }

    public RaftException(Throwable cause) {
        super(cause);
    }

    public RaftException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
