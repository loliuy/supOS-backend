package com.supos.common.event.multicaster;

import com.supos.common.annotation.Description;
import com.supos.common.utils.I18nUtils;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

@Component(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
public class StatusAwareApplicationEventMultiCaster extends SimpleApplicationEventMulticaster {

    public Collection<ApplicationListener<?>> getApplicationListeners(ApplicationEvent event) {
        Collection<ApplicationListener<?>> listeners = super.getApplicationListeners(event, ResolvableType.forInstance(event));
        return listeners;
    }

    static class ListenerInfo {
        final String methodDesc;
        final String name;

        ListenerInfo(String methodDesc, String name) {
            this.methodDesc = methodDesc;
            this.name = name;
        }
    }

    private ConcurrentHashMap<ApplicationListener, String> listInfoMap = new ConcurrentHashMap<>();
    static Field methodField;

    static {
        try {
            Field mf = ApplicationListenerMethodAdapter.class.getDeclaredField("method");
            mf.setAccessible(true);
            methodField = mf;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
        EventStatusAware aware;
        if (event instanceof EventStatusAware) {
            aware = (EventStatusAware) event;
        } else {
            super.multicastEvent(event, eventType);
            return;
        }
        final EventStatusAware eventStatusAware = aware;
        ResolvableType type = (eventType != null ? eventType : ResolvableType.forInstance(event));
        Executor executor = getTaskExecutor();
        Collection<ApplicationListener<?>> listeners = getApplicationListeners(event, type);
        int n = 0;
        for (ApplicationListener listener : listeners) {
            if (listInfoMap.containsKey(listener)) {
                n++;
            } else if (listener instanceof ApplicationListenerMethodAdapter) {
                try {
                    Method method = (Method) methodField.get(listener);
                    Description description = method.getAnnotation(Description.class);
                    if (description == null) {
                        description = method.getDeclaringClass().getAnnotation(Description.class);
                    }
                    String desc;
                    if (description != null) {
                        desc = description.value();
                        desc = I18nUtils.getMessage(desc);
                    } else {
                        desc = method.getDeclaringClass().getSimpleName();
                    }
                    listInfoMap.put(listener, desc);
                    n++;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        final int TOTAL = n;
        int i = 0;
        if (executor != null) {
            for (ApplicationListener<?> listener : listeners) {
                final String lisName = listInfoMap.get(listener);
                if (lisName != null) {
                    final int INDEX = ++i;
                    executor.execute(() -> {
                        eventStatusAware.beforeEvent(TOTAL, INDEX, lisName);
                        try {
                            invokeListener(listener, event);
                            eventStatusAware.afterEvent(TOTAL, INDEX, lisName, null);
                        } catch (Throwable ex) {
                            eventStatusAware.afterEvent(TOTAL, INDEX, lisName, ex);
                        }
                    });
                } else {
                    executor.execute(() -> invokeListener(listener, event));
                }
            }

        } else {
            for (ApplicationListener<?> listener : listeners) {
                final String lisName = listInfoMap.get(listener);
                if (lisName != null) {
                    i++;
                    try {
                        eventStatusAware.beforeEvent(TOTAL, i, lisName);
                        invokeListener(listener, event);
                        eventStatusAware.afterEvent(TOTAL, i, lisName, null);
                    } catch (Throwable ex) {
                        eventStatusAware.afterEvent(TOTAL, i, lisName, ex);
                        throw ex;
                    }
                } else {
                    invokeListener(listener, event);
                }
            }
        }
    }

}
