package com.supos.common.adpater;

public interface TopicMessageConsumer {
    void onMessage(String topic, String payload);
}
