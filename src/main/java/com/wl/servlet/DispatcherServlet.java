package com.wl.servlet;

import com.wl.demo.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {

    //没有使用xml文件，读取properties文件，简单处理
    private Properties contextConfig = new Properties();
    //文件夹名字存储
    private List<String> classNames = new ArrayList<String>();
    //实例化bean
    private Map<String, Object> ioc = new HashMap<String, Object>();
    //mapping
    List<Handler> handlerMapping = new ArrayList<Handler>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件，参数是web.xml文件中的param-name
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.解析配置文件中的内容，扫描出所有相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3.把所有扫描的类实例化
        doInstance();
        //4.将实例化好的bean，进行依赖注入
        doAutowired();
        //5.mvc的内容----初始化handlerMapping
        initHandlerMapping();
        System.out.println("Wl MVC Framework 已经准备就绪啦，欢迎来戳我!!!");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(WlController.class)) {
                continue;
            }
            String url = "";
            //先得到类上面的WlRequestMapping
            if (clazz.isAnnotationPresent(WlRequestMapping.class)) {
                WlRequestMapping requestMapping = (WlRequestMapping) clazz.getAnnotation(WlRequestMapping.class);
                url = requestMapping.value();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(WlRequestMapping.class)) {
                    continue;
                }
                //得到方法上面的WlRequestMapping
                WlRequestMapping requestMapping = (WlRequestMapping) method.getAnnotation(WlRequestMapping.class);
                //完整的请求串
                String customRegex = ("/" + url + requestMapping.value()).replaceAll("/+", "/");
                String regex = customRegex.replaceAll("\\*", ".*");

                //key=参数值或者名，value=index
                Map<String, Integer> pm = new HashMap<String, Integer>();
                Annotation[][] annotations = method.getParameterAnnotations();
                for (int i = 0; i < annotations.length; i++) {
                    for (Annotation a : annotations[i]) {
                        if (a instanceof WlrequestParam) {
                            String paramValue = ((WlrequestParam) a).value();
                            if (!"".equals(paramValue)) {
                                pm.put(paramValue, i);
                            }
                        }
                    }
                }

                //提取request和response的索引
                Class<?>[] paramTypes = method.getParameterTypes();
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> type = paramTypes[i];
                    if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                        pm.put(type.getName(), i);
                    }
                }
                handlerMapping.add(new Handler(Pattern.compile(regex), entry.getValue(), method, pm));

                System.out.println("Mapping " + customRegex + "  " + method);
            }
        }
    }

    private void doAutowired() {
        if (this.ioc.isEmpty()) {
            return;
        }

        //beanName,instance
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //每个类里面有的属性，依赖注入就是堆属性进行赋值
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(WlAutowired.class)) {
                    continue;
                }
                WlAutowired autowired = field.getAnnotation(WlAutowired.class);
                String beanName = autowired.value();
                //WlAutowired标签没有自己设置值，默认首字母小写
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                //第一个参数是实参，第二个参数是实例
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(WlController.class)) {
                    //beanName 通常是类名的首字母小写
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    this.ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(WlService.class)) {
                    //1.默认是首字母小写
                    WlService service = (WlService) clazz.getAnnotation(WlService.class);
                    String beanName = service.value();
                    //2.如果自己定义beanName,优先使用自己定义的
                    if ("".equals(beanName)) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    this.ioc.put(beanName, instance);

                    //3.如果注入的对象是接口，需要把实现类赋值给它
                    //找到这个类所有实现的接口
                    /*TestServiceImpl实现TestService，DemoService ，
                      上面testServiceImpl,value;testService,value;demoService,value;
                      三份如果实现类和接口名一样，两份，map对相同的key进行覆盖*/
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        this.ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String lowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String packageName) {
        //拿到类路径
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));

        File classDir = new File(url.getFile());

        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String location) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //拿请求的url找到对应的method
            boolean isMatch = pattern(req, resp);
            if (!isMatch) {
                resp.getWriter().write("404");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean pattern(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        boolean flag = false;
        if (handlerMapping.isEmpty()) {
            return false;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.pattern.matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            Class<?>[] paramTypes = handler.method.getParameterTypes();
            Object[] paramValues = new Object[paramTypes.length];
            Map<String, String[]> params = req.getParameterMap();

            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\]|\\[", "").replaceAll(",\\s", ",");
                if (!handler.paramMapping.containsKey(param.getKey())) {
                    continue;
                }
                int index = handler.paramMapping.get(param.getKey());
                //涉及到类型转换
                paramValues[index] = castStringValue(value, paramTypes[index]);
            }

            int reqIndex = handler.paramMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;

            int repIndex = handler.paramMapping.get(HttpServletResponse.class.getName());
            paramValues[repIndex] = resp;

            handler.method.invoke(handler.controller, paramValues);
            return true;
        }
        return flag;
    }

    private Object castStringValue(String value, Class<?> clazz) {
        if (clazz == String.class) {
            return value;
        } else if (clazz == Integer.class) {
            return Integer.valueOf(value);
        } else if (clazz == int.class) {
            return Integer.valueOf(value).intValue();
        } else {
            return null;
        }
    }


}
