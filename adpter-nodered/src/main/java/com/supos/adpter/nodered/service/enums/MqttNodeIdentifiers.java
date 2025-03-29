package com.supos.adpter.nodered.service.enums;

import com.supos.common.annotation.ProtocolIdentifierProvider;
import com.supos.common.dto.protocol.MqttConfigDTO;
import com.supos.common.dto.protocol.ProtocolTagEnums;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.exception.NodeRedException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅mqtt broker中所有topic
 */
@Slf4j
@Service("mqttNodeIdentifiers")
@ProtocolIdentifierProvider(IOTProtocol.MQTT)
public class MqttNodeIdentifiers implements IdentifiersInterface<MqttConfigDTO> {

    // key=clientId value=topic列表
    public static Map<String, Set<ProtocolTagEnums>> TOPIC_LIST_CACHE = new ConcurrentHashMap<>();
    // 所有在线的mqtt client
    public static Map<String, MqttAsyncClient> ACTIVE_CLIENT_LIST = new ConcurrentHashMap<>();

    @Override
    public Set<ProtocolTagEnums> listTags(MqttConfigDTO config, String topic) {
        String clientId = getClientId(config);

        connect(config, topic);

        try {
            Thread.sleep(3000); // 等一会订阅数据过来，产品要求订阅取数据 #_#
        } catch (InterruptedException e) {
            //
        }
        Set<ProtocolTagEnums> result = TOPIC_LIST_CACHE.getOrDefault(clientId, new HashSet<>());

        disconnect(config);

        return result;
    }

    private String getClientId(MqttConfigDTO config) {
        String broker = String.format("tcp://%s:%s", config.getServer().getHost(), config.getServer().getPort());
        return "supOSSubscriber_" + broker.hashCode();
    }

    private void connect(MqttConfigDTO config, String topic) {
        MemoryPersistence persistence = new MemoryPersistence();
        String broker = String.format("tcp://%s:%s", config.getServer().getHost(), config.getServer().getPort());
        String clientId = getClientId(config);
        MqttAsyncClient mqttClient = ACTIVE_CLIENT_LIST.get(clientId);
        if (mqttClient != null) {
            log.info("mqtt client已经存在， 直接返回， 不再重复订阅， host={}", config.getServer().getHost());
            return;
        }
        try {
            // 创建MQTT客户端
            MqttAsyncClient client = new MqttAsyncClient(broker, clientId, persistence);
            // 设置回调函数，用于处理接收到的消息
            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    Set<ProtocolTagEnums> topics = TOPIC_LIST_CACHE.getOrDefault(clientId, new HashSet<>());
                    topics.add(new ProtocolTagEnums(topic, ""));
                    TOPIC_LIST_CACHE.put(clientId, topics);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    // 不需要实现
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 不需要实现
                }
            });
            // 连接到MQTT Broker
            MqttConnectOptions connOpts = new MqttConnectOptions();
            if (StringUtils.hasText(config.getServer().getUsername()) && StringUtils.hasText(config.getServer().getPassword())) {
                connOpts.setUserName(config.getServer().getUsername());
                connOpts.setPassword(config.getServer().getPassword().toCharArray());
            }
            connOpts.setCleanSession(true); // 设置为true表示每次连接都是新的会话
            client.connect(connOpts).waitForCompletion();
            // 订阅主题
            try {
                client.subscribe(new String[]{"#"}, new int[] {0}); // 订阅所有topic
            } catch (MqttException e) {
                // ignore e
                //  改为订阅指定topic/#
                if (StringUtils.hasText(topic)) {
                    topic += topic.endsWith("/") ? "#" : "/#";
                } else {
                    topic = "/#";
                }
                client.subscribe(new String[]{topic}, new int[] {0});
            }
            // 放入本地缓存
            ACTIVE_CLIENT_LIST.put(clientId, client);
        } catch (Exception e) {
            log.error("连接mqtt broker（{}）失败", config.getServer().getHost(), e);
            throw new NodeRedException("连接mqtt失败", e);
        }
    }

    private void disconnect(MqttConfigDTO config) {
        String clientId = getClientId(config);
        MqttAsyncClient mqttClient = ACTIVE_CLIENT_LIST.get(clientId);
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            // 忽略exception
            log.error("mqtt client 断开连接异常， host={}", config.getServer().getHost());
        } finally {
            ACTIVE_CLIENT_LIST.remove(clientId);
            TOPIC_LIST_CACHE.remove(clientId);
        }
    }

}
