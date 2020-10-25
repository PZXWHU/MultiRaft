package com.pzx.rpc.factory;

import java.util.concurrent.*;

public class ThreadPoolFactory {

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAXIMUM_POOL_SIZE = 50;
    private static final int KEEP_ALIVE_TIME = 60;
    private static final int BLOCKING_QUEUE_CAPACITY = 100;

    private static volatile ThreadPoolExecutor defaultPool;
    private static volatile ThreadPoolExecutor asyncThreadPool;
    private static volatile ScheduledExecutorService scheduledThreadPool;



    public static ThreadPoolExecutor getDefaultPool(){
        if (defaultPool == null){
            synchronized (ThreadPoolFactory.class){
                if (defaultPool == null)
                    defaultPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPACITY), Executors.defaultThreadFactory());
            }
        }
        return defaultPool;
    }

    public static ScheduledExecutorService getScheduledThreadPool(){
        if (scheduledThreadPool == null){
            synchronized (ThreadPoolFactory.class){
                if (scheduledThreadPool == null)
                    scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
            }
        }
        return scheduledThreadPool;
    }

    public static ThreadPoolExecutor getAsyncThreadPool(){
        if (asyncThreadPool == null) {
            synchronized (ThreadFactory.class) {
                if (asyncThreadPool == null) {
                    asyncThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPACITY), Executors.defaultThreadFactory());;
                }
            }
        }
        return asyncThreadPool;
    }

    public static void close(){
        if (defaultPool != null)
            defaultPool.shutdown();
        if (asyncThreadPool != null)
            asyncThreadPool.shutdown();
        if (scheduledThreadPool!= null)
            scheduledThreadPool.shutdown();
    }

}
