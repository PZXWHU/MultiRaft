package com.pzx.rpc.exception;

import com.pzx.rpc.enumeration.RpcError;

public class RpcConnectException extends Exception {

    public RpcConnectException() {
    }

    public RpcConnectException(String message) {
        super(message);
    }

    public RpcConnectException(RpcError rpcError){
        this(rpcError.getMessage());
    }

    public RpcConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcConnectException(Throwable cause) {
        super(cause);
    }

    public RpcConnectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
