package com.dust.registry;

import com.dust.loadbalance.LoadBalancer;

import java.net.InetSocketAddress;

public interface ServiceRegistry {
    void register(String serviceName, InetSocketAddress inetSocketAddress);
    InetSocketAddress lookupService(String serviceName, LoadBalancer loadBalancer);
}
