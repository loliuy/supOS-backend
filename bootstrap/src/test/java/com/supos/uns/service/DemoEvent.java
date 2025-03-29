package com.supos.uns.service;

import com.supos.common.event.multicaster.EventStatusAware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
public class DemoEvent extends ApplicationEvent implements EventStatusAware {
    public DemoEvent(Object source) {
        super(source);
    }

    long t0;

    @Override
    public void beforeEvent(int totalListeners, int i, String listenerName) {
        log.info("before[{}/{}]: {}", i, totalListeners, listenerName);
        t0 = System.currentTimeMillis();
    }

    @Override
    public void afterEvent(int totalListeners, int i, String listenerName, Throwable err) {
        long t1 = System.currentTimeMillis();
        log.info("after[{}/{}]-[{} ms]: {},err={}", i, totalListeners, t1 - t0, listenerName, err);
    }
}
