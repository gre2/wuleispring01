package com.wl.servlet;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

public class Handler {
    protected Object controller;
    protected Method method;
    protected Pattern pattern;
    protected Map<String, Integer> paramMapping;

    public Handler(Pattern pattern, Object controller, Method method, Map<String, Integer> paramMapping) {
        this.pattern = pattern;
        this.controller = controller;
        this.method = method;
        this.paramMapping = paramMapping;
    }
}
