package com.supos.adapter.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MqttTest {
    @Test
    public void testMqttPublish() {
//        String topic = "/hkx/dev/1";
        Map<String, String[]> topicMsg = new HashMap<>();
//        {
//            String topic = "/motbus/9";
//            String[] contents = new String[]{
//                    "{\n" +
//                            " \"0\":{\"v\":\"supos\",\"q\":1}\n" +
//                            " ,\"2\":{\"v\":\"5.0\",\"q\":1,\"t\":1731561954214}\n" +
//                            "}",
//                    "{\"0\":{\"v\":\"Lite\",\"q\":100}, \"2\":{\"v\":\"3.6.0\",\"q\":1,\"t\":1731561954214} }",
//                    "[13.2,28.3]",
//            };
//            topicMsg.put(topic, contents);
//        }
        {
            String topic = "/hseqd/dev4" ;
            String[] contents = new String[]{
                    String.format("{\"wq\":%s, \"tm\":\"%s\"}", 20.12, 1.5),
                    String.format("{\"wq\":%s, \"tm\":\"%s\"}", 30.13, -3.4),
                    String.format("{\"wq\":%s, \"tm\":\"%s\"}", 612.95, 69.15926),
            };
            topicMsg.put(topic, contents);
        }
        {
            String topic = "/11/a/b/c/d/e/r/t";
            String[] contents = new String[]{
                    String.format("{\"wq\":%d, \"vaf\":\"%s\"}", 1, 1.3),
                    String.format("{\"wq\":%d, \"vaf\":\"%s\"}", 2, 1.4),
                    String.format("{\"wq\":%d, \"vaf\":\"%s\"}", 3, 1.5),
            };
            topicMsg.put(topic, contents);
        }
//        {
//            String topic = "/_origin/none";
//            String[] contents = new String[]{"10","12","25"
//            };
//            topicMsg.put(topic, contents);
//        }
        {
            String topic = "/ddx/1";
            String[] contents = new String[]{"10","12","25"
            };
            topicMsg.put(topic, contents);
        }
        {
            String topic = "/ddy/1";
            String[] contents = new String[]{"10","12","25"
            };
            topicMsg.put(topic, contents);
        }
        {
            String topic = "/ddy/2";
            String[] contents = new String[]{"12","12","25"
            };
            topicMsg.put(topic, contents);
        }


//        content = "[23.5,18.2]";
        int qos = 2;
        String broker = "tcp://100.100.100.20:31017";
        String clientId = "JavaSample";
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient sampleClient = null;
        try {
            sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setConnectionTimeout(5);
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            for (Map.Entry<String, String[]> entry : topicMsg.entrySet()) {
                String topic = entry.getKey();
                for (String content : entry.getValue()) {
                    MqttMessage message = new MqttMessage(content.getBytes());
                    message.setQos(qos);
//                message.setRetained(true);
                    sampleClient.publish(topic, message);
                }
            }
            System.out.println(new Date() + " Message published to [" + topicMsg.keySet() + "]");
            System.out.println("Disconnected");
            System.exit(0);
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        } finally {
            if (sampleClient != null) {
                try {
                    sampleClient.disconnect();
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Test
    public void testMqttConsume() {
        String broker = "tcp://100.100.100.20:31017"; // EMQX 服务器地址
        String clientId = "JavaClient";
        String topic = "/hkx/dev/1";

        try {
            // 创建 MQTT 客户端
            MqttClient client = new MqttClient(broker, clientId);

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            // 连接到 EMQX 服务器
            client.connect(options);
            System.out.println("Connected to broker: " + broker);

            // 设置消息回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost! " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Message arrived from topic: " + topic);
                    System.out.println("Message: " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 不需要处理
                }
            });

            // 订阅主题
            client.subscribe(topic);
            System.out.println("Subscribed to topic: " + topic);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}
