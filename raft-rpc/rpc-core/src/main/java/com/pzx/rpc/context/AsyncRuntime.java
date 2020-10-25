package com.pzx.rpc.context;

import com.pzx.rpc.factory.ThreadPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * 异步执行运行时
 *

 */
public class AsyncRuntime {

    private final static Logger LOGGER = LoggerFactory.getLogger(AsyncRuntime.class);

    private static volatile ThreadPoolExecutor asyncThreadPool;

    /**
     * 得到callback用的线程池
     *
     * @return callback用的线程池
     */
    public static ThreadPoolExecutor getAsyncThreadPool() {
        return ThreadPoolFactory.getAsyncThreadPool();
    }
}

