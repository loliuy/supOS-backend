package com.supos.common.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2024/12/11 15:17
 */
public class NamespaceChangeEvent extends ApplicationEvent {

    public String topic;

    public Map<String, Object> data;

    public NamespaceChangeEvent(Object source, String topic, Map<String, Object> data) {
        super(source);
        this.topic = topic;
        this.data = data;
    }

    public NamespaceChangeEvent(Object source) {
        super(source);
    }
}
