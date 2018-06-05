package proxy.staticproxy;

import proxy.common.ProxyInterface;
import proxy.common.RealObject;

//代理者
public class ProxyObject implements ProxyInterface {
    @Override
    public void say() {
        System.out.println("ProxyObject say start");
        new RealObject().say();
        System.out.println("ProxyObject say start");
    }
}
