package com.pzx.rpc.invoke;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;

import java.util.concurrent.Executors;

public interface RpcResponseCallBack {

    void onResponse(Object data);

    void onException(Throwable throwable);

    default Executors getExecutor(){
        return null;
    }

}
