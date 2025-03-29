package com.supos.common.event;

import com.supos.common.dto.FieldDefine;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class TopicMessageEvent extends ApplicationEvent {
    public final String topic;
    public final String payload;

    public final String protocol; // 协议名称
    public final Map<String, Object> data;
    public final Map<String, Object> lastData;
    public final Map<String, Long> lastDataTime;
    public final Map<String, FieldDefine> fieldsMap;
    public final long nowInMills;
    public final String err;

    public TopicMessageEvent(Object source, String topic, String payload) {
        super(source);
        this.fieldsMap = null;
        this.topic = topic;
        this.data = null;
        this.lastData = null;
        this.lastDataTime = null;
        this.payload = payload;
        this.nowInMills = System.currentTimeMillis();
        this.err = null;
        this.protocol = null;
    }

    public TopicMessageEvent(Object source, Map<String, FieldDefine> fieldsMap, String topic, String protocol, Map<String, Object> data,
                             Map<String, Object> lastData, Map<String, Long> lastDataTime, String payload, long now, String err) {
        super(source);
        this.fieldsMap = fieldsMap;
        this.topic = topic;
        this.data = data;
        this.lastData = lastData;
        this.lastDataTime = lastDataTime;
        this.payload = payload;
        this.nowInMills = now;
        this.err = err;
        this.protocol = protocol;
    }
}
