package com.pzx.raft.core.utils;

import com.pzx.rpc.factory.ThreadPoolFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class ThreadPoolUtils {

    private static volatile ScheduledExecutorService scheduledThreadPool;
    private static volatile ExecutorService executorThreadPool;
    private static Set<ThreadPoolExecutor> threadPools = new HashSet<>();

    public static ScheduledExecutorService getScheduledThreadPool(){
        if (scheduledThreadPool == null){
            synchronized (ThreadPoolFactory.class){
                if (scheduledThreadPool == null)
                    scheduledThreadPool = Executors.newScheduledThreadPool(5);
            }
        }
        return scheduledThreadPool;
    }

    public static ExecutorService getExecutorThreadPool(){
        if (executorThreadPool == null){
            synchronized (ThreadPoolFactory.class){
                if (executorThreadPool == null)
                    executorThreadPool = Executors.newScheduledThreadPool(5);
            }
        }
        return executorThreadPool;
    }

    public static ThreadPoolExecutor newThreadPoolExecutor(int corePoolSize,
                                                           int maximumPoolSize,
                                                           long keepAliveTime,
                                                           TimeUnit unit,
                                                           BlockingQueue<Runnable> workQueue){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        threadPools.add(threadPoolExecutor);
        return threadPoolExecutor;
    }

}
