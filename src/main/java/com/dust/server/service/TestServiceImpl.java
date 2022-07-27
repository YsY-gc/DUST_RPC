package com.dust.server.service;

import com.dust.annotion.Service;

@Service
public class TestServiceImpl implements TestService{
    @Override
    public int test() {
        return 0;
    }
}
