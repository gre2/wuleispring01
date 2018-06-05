package proxy.staticproxy;

import proxy.common.ProxyInterface;

public class MainClass {

    public static void consumer(ProxyInterface proxyInterface) {
        proxyInterface.say();
    }


    public static void main(String[] args) {
        consumer(new ProxyObject());
    }
}
