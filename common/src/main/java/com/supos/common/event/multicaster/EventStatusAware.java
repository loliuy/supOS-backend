package com.supos.common.event.multicaster;

public interface EventStatusAware {

    void beforeEvent(int totalListeners, int i, String listenerName);

    void afterEvent(int totalListeners, int i, String listenerName, Throwable err);

}
