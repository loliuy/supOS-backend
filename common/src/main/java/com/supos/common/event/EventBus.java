package com.supos.common.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EventBus implements ApplicationEventPublisherAware {
    private ApplicationEventPublisher publisher;
    private static EventBus instance;

    @PostConstruct
    void init() {
        instance = this;
    }

    public static void publishEvent(ApplicationEvent event) {
        instance.publisher.publishEvent(event);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        publisher = applicationEventPublisher;
    }
}
