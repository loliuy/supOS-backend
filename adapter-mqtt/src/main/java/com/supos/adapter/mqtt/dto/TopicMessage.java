package com.supos.adapter.mqtt.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TopicMessage {
    String topic;
    List<Map<String, Object>> msg;

    public TopicMessage(){
    }
    public TopicMessage(String topic, List<Map<String, Object>> msg) {
        this.topic = topic;
        this.msg = msg;
    }
}
