package com.dust.client;

import com.dust.client.handler.RpcResponseMessageHandler;
import com.dust.loadbalance.LoadBalancer;
import com.dust.loadbalance.RoundRobinLoadBalancer;
import com.dust.message.RpcRequestMessage;
import com.dust.protocol.MessageCodecSharable;
import com.dust.protocol.ProcotolFrameDecoder;
import com.dust.protocol.SequenceIdGenerator;
import com.dust.registry.NacosServiceRegistry;
import com.dust.server.service.HelloService;
import com.dust.server.service.TestService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

@Slf4j
public class RpcClientManager {


    public static void main(String[] args) {
        HelloService helloService = getProxyService(HelloService.class);
        System.out.println(helloService.sayHello("ZhangSan"));
        TestService testService = getProxyService(TestService.class);
        System.out.println(testService.test());
    }

    // 创建代理类
    public static <T> T getProxyService(Class<T> serviceClass) {
        ClassLoader loader = serviceClass.getClassLoader();
        Class<?>[] interfaces = new Class[]{serviceClass};
        //                                                           sayHello "张三"
        Object o = Proxy.newProxyInstance(loader, interfaces, (proxy, method, args) -> {
            // 1. 将方法调用转换为 消息对象
            int sequenceId = SequenceIdGenerator.nextId();
            RpcRequestMessage msg = new RpcRequestMessage(
                    sequenceId,
                    serviceClass.getName(),
                    method.getName(),
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args
            );
            // 2. 将消息对象发送出去
            //从nacos中获取服务地址
            //负载均衡器，查询服务时遵循负载均衡策略
            LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
            InetSocketAddress address = new NacosServiceRegistry().lookupService(serviceClass.getCanonicalName(),loadBalancer);
            Channel channel = getChannel(address.getHostName(), address.getPort());
            channel.writeAndFlush(msg);

            // 3. 准备一个空 Promise 对象，来接收结果   指定 promise 对象异步接收结果线程
            DefaultPromise<Object> promise =
                    new DefaultPromise<>(channel.eventLoop());

            RpcResponseMessageHandler.PROMISES.put(sequenceId, promise);

            // promise.addListener(future -> {
            //     // 线程
            // });

            // 4. 等待 promise 结果
            promise.await();
            if(promise.isSuccess()) {
                // 调用正常
                return promise.getNow();
            } else {
                // 调用失败
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) o;
    }

    private static Channel channel = null;
    private static final Object LOCK = new Object();

    // 单例模式，获取唯一的 channel 对象
    public static Channel getChannel(String inetHost, int inetPort) {
        if (channel != null) {
            return channel;
        }
        synchronized (LOCK) {
            if (channel != null) {
                return channel;
            }
            initChannel(inetHost, inetPort);
            return channel;
        }
    }

    // 初始化 channel 方法
    private static void initChannel(String inetHost, int inetPort) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ProcotolFrameDecoder());
                ch.pipeline().addLast(LOGGING_HANDLER);
                ch.pipeline().addLast(MESSAGE_CODEC);
                ch.pipeline().addLast(RPC_HANDLER);
            }
        });
        try {
            channel = bootstrap.connect(inetHost, inetPort).sync().channel();
            channel.closeFuture().addListener(future -> {
                group.shutdownGracefully();
            });
        } catch (Exception e) {
            log.error("client error", e);
        }
    }
}