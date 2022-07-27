package com.dust.protocol;

/**
 * 生成请求消息序列号的工具类（tcp滑动窗口）
 */
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SequenceIdGenerator {
    private static final AtomicInteger id = new AtomicInteger();

    public static int nextId() {
        return id.incrementAndGet();
    }
}
