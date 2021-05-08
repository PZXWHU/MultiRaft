package com.pzx.rpc.transport.netty.client;

import com.pzx.rpc.context.AsyncRuntime;
import com.pzx.rpc.context.RpcInvokeContext;
import com.pzx.rpc.entity.RpcRequest;
import com.pzx.rpc.entity.RpcResponse;
import com.pzx.rpc.enumeration.ResponseCode;
import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcConnectException;
import com.pzx.rpc.exception.RpcException;
import com.pzx.rpc.factory.DaemonThreadFactory;
import com.pzx.rpc.serde.RpcSerDe;
import com.pzx.rpc.service.provider.ServiceProvider;
import com.pzx.rpc.service.registry.ServiceRegistry;
import com.pzx.rpc.transport.RpcClient;
import com.pzx.rpc.transport.netty.codec.ProtocolNettyDecoder;
import com.pzx.rpc.transport.netty.codec.ProtocolNettyEncoder;
import com.pzx.rpc.transport.netty.server.NettyServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NettyClient implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private final InetSocketAddress serverAddress;
    private final RpcSerDe rpcSerDe;
    private final ServiceRegistry serviceRegistry;
    private final ExecutorService executorService;

    public NettyClient(InetSocketAddress serverAddress) {
        this(serverAddress,  null);
    }

    public NettyClient(ServiceRegistry serviceRegistry) {
        this(null,  serviceRegistry);
    }

    private NettyClient(InetSocketAddress serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.rpcSerDe = RpcSerDe.getByCode(DEFAULT_SERDE_CODE);
        this.serviceRegistry = serviceRegistry;
        this.executorService = Executors.newFixedThreadPool(1, new DaemonThreadFactory());
    }

    @Override
    public CompletableFuture<RpcResponse> sendRequest(RpcRequest rpcRequest) {

        CompletableFuture<RpcResponse> resultFuture = new CompletableFuture<>();
        RpcInvokeContext.putUncompletedFuture(rpcRequest.getRequestId(), resultFuture);
        //异步发送
        executorService.submit(()->{
            InetSocketAddress requestAddress = serviceRegistry != null ? serviceRegistry.lookupService(rpcRequest.getInterfaceName()) : serverAddress;
            try {

                Channel channel = ChannelPool.get(requestAddress, rpcSerDe);
                /*if (rpcRequest.getMethodName().equals("regionHeartbeat"))
                    System.out.println("开始发送："+ rpcRequest.getRequestId() + "  " + LocalDateTime.now());*/
                channel.writeAndFlush(rpcRequest).addListener((ChannelFuture future1) -> {
                    if(future1.isSuccess()) {
                        logger.debug(String.format("客户端发送消息: %s", rpcRequest.toString()));
                    } else {
                        future1.channel().close();
                        RpcInvokeContext.completeFutureExceptionally(rpcRequest.getRequestId(), future1.cause());
                    }
                    /*if (rpcRequest.getMethodName().equals("regionHeartbeat"))
                        System.out.println("完成发送："+ rpcRequest.getRequestId() + "  " + LocalDateTime.now() + "  " + future1.isSuccess());*/
                });
                /*if (rpcRequest.getMethodName().equals("regionHeartbeat"))
                    System.out.println("发送完成:" + rpcRequest.getRequestId());*/
            } catch (InterruptedException | RpcConnectException e) {
                RpcInvokeContext.completeFutureExceptionally(rpcRequest.getRequestId(),  e);
            }
        });

        return resultFuture;

    }
}
