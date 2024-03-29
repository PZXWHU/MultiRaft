package com.pzx.rpc.transport.netty.client;

import com.pzx.rpc.enumeration.RpcError;
import com.pzx.rpc.exception.RpcConnectException;
import com.pzx.rpc.factory.ThreadPoolFactory;
import com.pzx.rpc.serde.RpcSerDe;
import com.pzx.rpc.transport.netty.codec.ProtocolNettyDecoder;
import com.pzx.rpc.transport.netty.codec.ProtocolNettyEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class ChannelPool {

    private static final Logger logger = LoggerFactory.getLogger(ChannelPool.class);
    private static final Bootstrap bootstrap = initializeBootstrap();
    private static EventLoopGroup eventLoopGroup;
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
    private static Map<String, Channel> channels = new ConcurrentHashMap<>();

    private static Set<String> connectingChannels = ConcurrentHashMap.newKeySet();
    private static Map<String, InetSocketAddress> blackList = new ConcurrentHashMap<>();
    static {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            for (Map.Entry<String, InetSocketAddress> entry : blackList.entrySet()) {
                if (connectingChannels.contains(entry.getKey()))
                    continue;
                connectingChannels.add(entry.getKey());
                bootstrap.connect(entry.getValue()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            channels.put(entry.getKey(), channelFuture.channel());
                            blackList.remove(entry.getKey());
                        } else {
                            channelFuture.channel().close();
                        }
                        connectingChannels.remove(entry.getKey());
                    }
                });
            }
        }, 3,3, TimeUnit.SECONDS);
    }

    /**
     * 当连接失败时，返回null
     * @param inetSocketAddress
     * @param rpcSerDe
     * @return
     * @throws InterruptedException
     */
    public static Channel get(InetSocketAddress inetSocketAddress, RpcSerDe rpcSerDe) throws InterruptedException, RpcConnectException{
        String key = (inetSocketAddress.toString() + rpcSerDe.getCode()).intern();//获取字符串常量池中的对象
        //当出现key相同时，由于字符串常量池的存在，相同key会是同一个对象
        synchronized (key){

            if (blackList.containsKey(key)){
                throw new RpcConnectException(RpcError.BAD_CONNECTION);
            }

            if (channels.containsKey(key)) {
                Channel channel = channels.get(key);
                if(channels != null && channel.isActive()) {
                    return channel;
                } else {
                    channels.remove(key);
                }
            }

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new ProtocolNettyDecoder())
                            .addLast(new ProtocolNettyEncoder(rpcSerDe))
                            .addLast(new RpcResponseInboundHandler());
                }
            });

            CountDownLatch countDownLatch = new CountDownLatch(1);
            Throwable connectThrowable = new Throwable();
            bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()){
                        channels.put(key, channelFuture.channel());
                    }else {
                        blackList.put(key, inetSocketAddress);
                        channelFuture.channel().close();
                        connectThrowable.initCause(channelFuture.cause());
                    }
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
            if (channels.get(key) == null)
                throw new RpcConnectException(connectThrowable.getCause().getMessage());
        }

        return channels.get(key);

    }

    public static void stop(){
        if (eventLoopGroup != null)
            eventLoopGroup.shutdownGracefully();
        if (scheduledExecutorService != null)
            scheduledExecutorService.shutdown();
        for(Channel channel : channels.values()){
            channel.close();
        }
    }

    private static Bootstrap initializeBootstrap() {
        eventLoopGroup = new NioEventLoopGroup(5);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                //连接的超时时间，超过这个时间还是建立不上的话则代表连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                //是否开启 TCP 底层心跳机制
                .option(ChannelOption.SO_KEEPALIVE, true)
                //TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                .option(ChannelOption.TCP_NODELAY, true);
        return bootstrap;
    }


}
