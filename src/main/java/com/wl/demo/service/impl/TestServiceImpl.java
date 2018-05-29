package com.wl.demo.service.impl;

import com.wl.demo.annotation.WlService;
import com.wl.demo.service.TestService;

@WlService
public class TestServiceImpl implements TestService {


    @Override
    public String queryName(String name) {
        return "my name is " + name;
    }
}
