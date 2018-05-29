package com.wl.demo.controller;

import com.wl.demo.annotation.WlAutowired;
import com.wl.demo.annotation.WlController;
import com.wl.demo.annotation.WlRequestMapping;
import com.wl.demo.annotation.WlrequestParam;
import com.wl.demo.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WlController
@WlRequestMapping("/test")
public class TestController {

    @WlAutowired
    private TestService testService;

    @WlRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @WlrequestParam("name") String name) {
        String result = testService.queryName(name);
        try {
            response.getWriter().write(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
