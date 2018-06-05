package proxy.dynamic;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ProxyObject implements InvocationHandler {

    private Object proxy;

    public ProxyObject() {
    }

    public ProxyObject(Object proxyRequest) {
        this.proxy = proxyRequest;
    }


    @Override
    public Object invoke(Object proxyObject, Method method, Object[] args) throws Throwable {
        return method.invoke(proxy, args);
    }
}
