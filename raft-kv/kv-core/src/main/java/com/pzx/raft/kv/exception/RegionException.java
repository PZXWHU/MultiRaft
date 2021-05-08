package com.pzx.raft.kv.exception;

public class RegionException extends RuntimeException{

    public RegionException() {
    }

    public RegionException(String message) {
        super(message);
    }

    public RegionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegionException(Throwable cause) {
        super(cause);
    }

    public RegionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
