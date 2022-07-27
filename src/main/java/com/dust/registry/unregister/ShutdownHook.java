package com.dust.registry.unregister;

import com.dust.registry.utils.NacosUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ShutdownHook {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(2);
    private static final ShutdownHook shutdownHook = new ShutdownHook();

    public static ShutdownHook getShutdownHook() {
        return shutdownHook;
    }

    public void addClearAllHook() {
        log.info("关闭后将自动注销所有服务");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NacosUtil.clearRegistry();
            log.info("注销所有服务");
            threadPool.shutdown();
        }));
    }
}

