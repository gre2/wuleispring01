package proxy.dynamic;

import proxy.common.ProxyInterface;
import proxy.common.RealObject;

import java.lang.reflect.Proxy;

public class MainClass {

    public static void consumer(ProxyInterface proxyInterface) {
        proxyInterface.say();
    }

    public static void main(String[] args) {
        RealObject realObject = new RealObject();
        //Invocation 调用   handler 处理程序
        ProxyInterface interf =
                (ProxyInterface) Proxy.newProxyInstance(ProxyInterface.class.getClassLoader(), new Class[]{ProxyInterface.class}, new ProxyObject(realObject));

        consumer(interf);
    }
}
