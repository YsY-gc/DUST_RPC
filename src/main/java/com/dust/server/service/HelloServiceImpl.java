package com.dust.server.service;

import com.dust.annotion.Service;

@Service
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String msg) {
        return "你好, " + msg;
    }
}
