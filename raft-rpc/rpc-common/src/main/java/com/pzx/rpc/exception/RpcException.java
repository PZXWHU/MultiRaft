package com.pzx.rpc.exception;

import com.pzx.rpc.enumeration.RpcError;

public class RpcException extends RuntimeException {

    public RpcException(RpcError error, String errorDetail) { super(error.getMessage() + ": " + errorDetail); }

    public RpcException(RpcError error) {
        super(error.getMessage());
    }

    public RpcException(String message, Throwable throwable) {super(message, throwable);}

    public RpcException() {
    }

    public RpcException(Throwable cause) {
        super(cause);
    }
}
