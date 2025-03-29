package com.supos.adpter.nodered.schedule;

import com.supos.adpter.nodered.service.ObjectCachePool;
import com.supos.adpter.nodered.service.enums.MqttNodeIdentifiers;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CacheCleanScheduler {

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void clearObjectCache() {
        ObjectCachePool.IOT_PROTOCOL_MAP.clear();
        ObjectCachePool.SERVER_MAP.clear();
    }

    /**
     * 12小时清理一次MQTT CLIENT缓存， 防止内存泄漏
     */
    @Scheduled(fixedRate = 12 * 60 * 60 * 1000)
    public void clearMQTTClientCache() {
        for (Map.Entry<String, MqttAsyncClient> entry : MqttNodeIdentifiers.ACTIVE_CLIENT_LIST.entrySet()) {
            if (entry.getValue().isConnected()) {
                try {
                    entry.getValue().disconnect();
                } catch (MqttException e) {
                    // ignore
                }
            }
        }
        MqttNodeIdentifiers.ACTIVE_CLIENT_LIST.clear();
        MqttNodeIdentifiers.TOPIC_LIST_CACHE.clear();
    }
}
