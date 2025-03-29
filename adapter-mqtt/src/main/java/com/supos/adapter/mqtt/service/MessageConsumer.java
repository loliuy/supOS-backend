package com.supos.adapter.mqtt.service;

import com.supos.adapter.mqtt.dto.TopicDefinition;

import java.util.Map;

public interface MessageConsumer {

    boolean isSendTopic();
    void setSendTopic(boolean sendTopic);

    long getQueueFrontIndex();

    long getQueueTailIndex();

    long getQueueSize();

    long getEnqueuedSize();

    long getDequeuedSize();

    long getPublishedCalcSize();

    long getPublishedMergedSize();

    long getArrivedCalcSize();

    String getQueueHead();

    Map<String, TopicDefinition> getTopicDefinitionMap();

    void onMessage(String topic, int msgId, String payload);
}
