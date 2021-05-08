package com.pzx.rpc.transport.netty.server;

import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.enumeration.ResponseCode;
import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.factory.SingletonFactory;
import com.pzx.rpc.service.handler.DefaultServiceRequestHandler;
import com.pzx.rpc.service.handler.ServiceRequestHandler;
import com.pzx.rpc.service.provider.ServiceProvider;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class RpcRequestInboundHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcRequestInboundHandler.class);
    private final ServiceRequestHandler defaultServiceRequestHandler;
    private final ServiceProvider serviceProvider;

    public RpcRequestInboundHandler(ServiceProvider serviceProvider) {

        this.defaultServiceRequestHandler = new DefaultServiceRequestHandler(serviceProvider);
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {

        logger.debug("服务器接收到请求: {}  {}", rpcRequest, LocalDateTime.now());
        CompletableFuture<RpcResponse> future = defaultServiceRequestHandler.handleSync(rpcRequest);
        future.whenComplete((rpcResponse, throwable) -> {
           if (throwable != null){
               logger.warn(throwable.getMessage());
               System.out.println("RPC调用失败！");
               rpcResponse = RpcResponse.fail(rpcRequest.getRequestId(), ResponseCode.UNKNOWN_ERROR);
           }
           ctx.channel().writeAndFlush(rpcResponse).addListeners(new ChannelFutureListener() {
               @Override
               public void operationComplete(ChannelFuture channelFuture) throws Exception {
               /*    if (channelFuture.isSuccess())
                       System.out.println("返回请求完成 ：" + rpcRequest + "  " + channelFuture.isSuccess() + "  " + channelFuture.cause() + "  " + rpcResponse);
                   else
                       System.out.println("返回请求失败 ：" + rpcRequest + "  " + channelFuture.isSuccess() + "  " + channelFuture.cause() + "  " + rpcResponse);
               */
               }
           });

        });
        /*if (rpcRequest.getMethodName().equals("regionHeartbeat"))
            System.out.println("开始返回请求 ：" + rpcRequest);
        RpcResponse rpcResponse = defaultServiceRequestHandler.handle(rpcRequest);
        ctx.channel().writeAndFlush(rpcResponse).addListeners(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (rpcRequest.getMethodName().equals("regionHeartbeat"))
                    System.out.println("返回请求完成 ：" + rpcRequest + "  " + channelFuture.isSuccess() + "  " + channelFuture.cause() + "  " + rpcResponse);
            }
        });*/

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().toString() + " 已连接");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().toString() + " 已关闭");
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("处理过程调用时有错误发生:");
        cause.printStackTrace();
        ctx.close();
    }
}
