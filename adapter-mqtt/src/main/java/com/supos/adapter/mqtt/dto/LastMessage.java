package com.supos.adapter.mqtt.dto;

import cn.hutool.core.bean.BeanUtil;
import com.supos.adapter.mqtt.util.DateUtil;
import lombok.Data;

@Data
public class LastMessage implements Cloneable {
    String topic;
    Long timestamp;
    Integer messageId;
    String payload;

    public void update(String topic, int messageId, String payload) {
        this.topic = topic;
        this.timestamp = System.currentTimeMillis();
        this.messageId = messageId;
        this.payload = payload;
    }

    @Override
    public LastMessage clone() {
        try {
            return (LastMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            LastMessage msg = new LastMessage();
            BeanUtil.copyProperties(this, msg);
            return msg;
        }
    }

    public String getArrivedTime() {
        return DateUtil.dateStr(timestamp);
    }
}
