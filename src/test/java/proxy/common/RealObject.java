package proxy.common;


//被代理者
public class RealObject implements ProxyInterface {

    @Override
    public void say() {
        System.out.println("RealObject say hello");
    }
}
