package thomas.swisher.utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import lombok.Value;

/**
 * Creates interface implementations backed by
 * posts to EventBus
 */
public class AsyncProxy {

    @Value
    private static class MethodInvocation {
        public final Method method;
        public final Object[] args;
    }

    public static <I> I create(Class<I> interfaceX, EventBus eventBus) {
        return (I) Proxy.newProxyInstance(
            AsyncProxy.class.getClassLoader(),
            new Class[]{interfaceX},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    eventBus.post(new MethodInvocation(method, args));
                    return null;
                }
            });
    }

    public static class MethodInvocationListener<I> {
        private final Object listener = new Object() {
            @Subscribe(threadMode = ThreadMode.MAIN)
            public void onEvent(MethodInvocation methodInvocation) {
                try {
                    methodInvocation.method.invoke(forwardTo, methodInvocation.args);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        private I forwardTo;
        private EventBus eventBus;

        public MethodInvocationListener(I forwardTo, EventBus eventBus) {
            this.forwardTo = forwardTo;
            this.eventBus = eventBus;
            this.eventBus.register(listener);
        }
        public void finish() {
            this.eventBus.unregister(listener);
        }
    }
}
