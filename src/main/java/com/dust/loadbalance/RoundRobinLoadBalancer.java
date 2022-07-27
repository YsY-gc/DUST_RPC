package com.dust.loadbalance;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
/**
 * 轮询负载均衡器
 */
public class RoundRobinLoadBalancer implements LoadBalancer{
    private int index = 0;
    @Override
    public Instance select(List<Instance> instances) {
        if(index >= instances.size()) {
            index %= instances.size();
        }
        return instances.get(index++);
    }
}
