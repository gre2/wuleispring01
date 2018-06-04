# 手写springmvc

1.加载配置阶段

```china
1.web.xml
2.DispatcherServlet
  application.xml文件所在的路径,classpath:application.xml,通过什么url才能请求到/*
```

2.初始化阶段

```china
1.init
  通过init方法能够读取到web中所配置的信息
2.读取，解析
  spring知道对那些bean进行操作
3.初始化ioc容器
  把已经解析到的bean存放在map中 key：beanName  value：instance
4.进行依赖注入
  实现对属性进行动态赋值
5.handlerMapping
  主要是为了将一个url和method进行一一对应
```

3.等待请求阶段

```
1.doPost
  获取到参数和url
2.在handlerMapping匹配到method，利用反射去进行调用
3.通过response将结果输出
```
