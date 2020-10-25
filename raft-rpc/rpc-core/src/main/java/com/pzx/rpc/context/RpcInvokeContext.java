package com.pzx.rpc.context;

import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.invoke.RpcResponseCallBack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public class RpcInvokeContext {

    /**
     * 线程上下文变量
     */
    protected static final ThreadLocal<RpcInvokeContext> LOCAL = new ThreadLocal<RpcInvokeContext>();

    /**
     * 未完成的异步调用的Future
     */
    private static ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> uncompletedFutures = new ConcurrentHashMap<>();

    public static boolean containsUncompletedFuture(Integer requestId){
        return uncompletedFutures.contains(requestId);
    }

    /**
     * 增加未完成的异步调用的Future
     * @param requestId
     * @param future
     */
    public static void putUncompletedFuture(Integer requestId, CompletableFuture<RpcResponse> future){
        uncompletedFutures.put(requestId, future);
    }

    /**
     * 删除未完成的异步调用的Future
     * @param requestId
     */
    public static CompletableFuture removeUncompletedFuture(Integer requestId){
        return uncompletedFutures.remove(requestId);
    }

    /**
     * 删除并完成异步调用的Future
     * @param rpcResponse
     */
    public static void completeFuture(RpcResponse rpcResponse){

        CompletableFuture<RpcResponse> future = uncompletedFutures.remove(rpcResponse.getRequestId());
        if (null != future) {
            future.complete(rpcResponse);
        }
    }

    /**
     * 删除并完成异步调用的Future
     * @param throwable
     */
    public static void completeFutureExceptionally(Integer requestId, Throwable throwable){

        CompletableFuture<RpcResponse> future = uncompletedFutures.remove(requestId);
        if (null != future) {
            future.completeExceptionally(throwable);
        }
    }

    /**
     * 得到上下文，没有则初始化
     *
     * @return 调用上下文
     */
    public static RpcInvokeContext getContext() {
        RpcInvokeContext context = LOCAL.get();
        if (context == null) {
            context = new RpcInvokeContext();
            LOCAL.set(context);
        }
        return context;
    }

    /**
     * 删除上下文
     */
    public static void removeContext() {
        LOCAL.remove();
    }

    /**
     * 设置上下文
     *
     * @param context 调用上下文
     */
    public static void setContext(RpcInvokeContext context) {
        LOCAL.set(context);
    }

    /**
     * 用户自定义超时时间，单次调用生效
     */
    protected Integer timeout;

    /**
     * 用户自定义对方地址，单次调用生效
     */
    protected String targetURL;

    /**
     * 用户自定义Callback，单次调用生效
     */
    protected RpcResponseCallBack responseCallback;

    /**
     * The Future.
     */
    protected Future<?> future;

    /**
     * 自定义属性
     */
    protected ConcurrentMap<String, Object> map             = new ConcurrentHashMap<String, Object>();

    /**
     * 得到调用级别超时时间
     *
     * @return 超时时间
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * 设置调用级别超时时间
     *
     * @param timeout 超时时间
     * @return 当前
     */
    public RpcInvokeContext setTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * 获取单次请求的指定地址
     *
     * @return 单次请求的指定地址
     */
    public String getTargetURL() {
        return targetURL;
    }

    /**
     * 设置单次请求的指定地址
     *
     * @param targetURL 单次请求的指定地址
     * @return RpcInvokeContext
     */
    public RpcInvokeContext setTargetURL(String targetURL) {
        this.targetURL = targetURL;
        return this;
    }

    /**
     * 获取单次请求的指定回调方法
     *
     * @return 单次请求的指定回调方法
     */
    public RpcResponseCallBack getResponseCallback() {
        return responseCallback;
    }

    /**
     * 设置单次请求的指定回调方法
     *
     * @param responseCallback 单次请求的指定回调方法
     * @return RpcInvokeContext
     */
    public RpcInvokeContext setResponseCallback(RpcResponseCallBack responseCallback) {
        this.responseCallback = responseCallback;
        return this;
    }

    /**
     * 得到单次请求返回的异步Future对象
     *
     * @param <T> 返回值类型
     * @return 异步Future对象
     */
    @SuppressWarnings("unchecked")
    public <T> Future<T> getFuture() {
        return (Future<T>) future;
    }

    /**
     * 设置单次请求返回的异步Future对象
     *
     * @param future Future对象
     * @return RpcInvokeContext
     */
    public RpcInvokeContext setFuture(Future<?> future) {
        this.future = future;
        return this;
    }

    /**
     * 设置一个调用上下文数据
     *
     * @param key   Key
     * @param value Value
     */
    public void put(String key, Object value) {
        if (key != null && value != null) {
            map.put(key, value);
        }
    }

    /**
     * 获取一个调用上下文数据
     *
     * @param key Key
     * @return 值
     */
    public Object get(String key) {
        if (key != null) {
            return map.get(key);
        }
        return null;
    }

    /**
     * 删除一个调用上下文数据
     *
     * @param key Key
     * @return 删除前的值
     */
    public Object remove(String key) {
        if (key != null) {
            return map.remove(key);
        }
        return null;
    }

}
