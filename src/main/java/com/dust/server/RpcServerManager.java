package com.dust.server;

import com.dust.annotion.ReflectUtil;
import com.dust.annotion.Service;
import com.dust.annotion.ServiceScan;
import com.dust.exception.RpcError;
import com.dust.exception.RpcException;
import com.dust.protocol.MessageCodecSharable;
import com.dust.protocol.ProcotolFrameDecoder;
import com.dust.registry.unregister.ShutdownHook;
import com.dust.server.handler.RpcRequestMessageHandler;
import com.dust.registry.register.ServicesFactoryAuto;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Rpc服务器
 */
@Slf4j
@ServiceScan("com.dust.server.service")
public class RpcServerManager {
    public static void main(String[] args) {
        //自动扫描包下所有注解Service的服务，并注册到本地（Map）和Nacos
        scanServices();
        //处理连接线程组，和处理业务线程组
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        //记录日志handler
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        //编解码协议handler
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        //request响应handler
        RpcRequestMessageHandler RPC_HANDLER = new RpcRequestMessageHandler();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ProcotolFrameDecoder());
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);
                    ch.pipeline().addLast(RPC_HANDLER);
                }
            });
            ChannelFuture future = serverBootstrap.bind(8080).sync();
            ShutdownHook.getShutdownHook().addClearAllHook();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    //包扫描，并注册到本地和Nacos
    public static void scanServices() {
        String mainClassName = ReflectUtil.getStackTrace();
        Class<?> startClass;
        try {
            startClass = Class.forName(mainClassName);
            if(!startClass.isAnnotationPresent(ServiceScan.class)) {
                log.error("启动类缺少 @ServiceScan 注解");
                throw new RpcException(RpcError.SERVICE_SCAN_PACKAGE_NOT_FOUND);
            }
        } catch (ClassNotFoundException e) {
            log.error("出现未知错误");
            throw new RpcException(RpcError.UNKNOWN_ERROR);
        }
        String basePackage = startClass.getAnnotation(ServiceScan.class).value();
        if("".equals(basePackage)) {
            basePackage = mainClassName.substring(0, mainClassName.lastIndexOf("."));
        }
        Set<Class<?>> classSet = ReflectUtil.getClasses(basePackage);
        for(Class<?> clazz : classSet) {
            if(clazz.isAnnotationPresent(Service.class)) {

                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> oneInterface: interfaces){
                    ServicesFactoryAuto.register(oneInterface, clazz);
                }

            }
        }
    }

}
