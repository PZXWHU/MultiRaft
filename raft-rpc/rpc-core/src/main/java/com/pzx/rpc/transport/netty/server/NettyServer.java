package com.pzx.rpc.transport.netty.server;

import com.pzx.rpc.annotation.Service;
import com.pzx.rpc.serde.RpcSerDe;
import com.pzx.rpc.service.provider.MemoryServiceProvider;
import com.pzx.rpc.service.provider.ServiceProvider;
import com.pzx.rpc.service.registry.ServiceRegistry;
import com.pzx.rpc.transport.AbstractRpcServer;
import com.pzx.rpc.transport.RpcServer;
import com.pzx.rpc.transport.netty.codec.ProtocolNettyDecoder;
import com.pzx.rpc.transport.netty.codec.ProtocolNettyEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.provider.certpath.PKIXTimestampParameters;

import java.net.InetSocketAddress;


public class NettyServer extends AbstractRpcServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final InetSocketAddress serverAddress;
    private final ServiceRegistry serviceRegistry;
    private final ServiceProvider serviceProvider;
    private final RpcSerDe rpcSerDe;
    private final boolean autoScanService;

    private NettyServer(Builder builder){
        this.serverAddress = builder.serverAddress;
        this.serviceRegistry = builder.serviceRegistry;
        this.serviceProvider =  new MemoryServiceProvider();
        this.rpcSerDe = RpcSerDe.getByCode(DEFAULT_SERDE_CODE);
        this.autoScanService = builder.autoScanService;
    }

    @Override
    public <T> void publishService(Object service, String serviceName) {
        serviceProvider.addService(service, serviceName);
        if (serviceRegistry != null)
            serviceRegistry.registerService(serviceName, serverAddress);
    }

    @Override
    public void start() {
        if (this.autoScanService)
            scanAndPublishServices();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(2);//业务线程池
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 256)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new ProtocolNettyEncoder(rpcSerDe));
                            pipeline.addLast(new ProtocolNettyDecoder());
                            pipeline.addLast(businessGroup, new RpcRequestInboundHandler(serviceProvider));
                        }
                    });
            ChannelFuture future = serverBootstrap.bind(serverAddress.getPort()).sync();
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            logger.error("启动服务器时有错误发生: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }

    public static class Builder{
        private final InetSocketAddress serverAddress;
        private ServiceRegistry serviceRegistry;
        private boolean autoScanService = true;

        public Builder(InetSocketAddress serverAddress) {
            this.serverAddress = serverAddress;
        }


        public Builder serviceRegistry(ServiceRegistry serviceRegistry){
            this.serviceRegistry = serviceRegistry;
            return this;
        }

        public Builder autoScanService(boolean autoScanService){
            this.autoScanService = autoScanService;
            return this;
        }

        public NettyServer build(){
            return new NettyServer(this);
        }

    }

}
