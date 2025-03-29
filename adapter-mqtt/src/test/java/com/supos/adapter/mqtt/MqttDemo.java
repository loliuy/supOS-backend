package com.supos.adapter.mqtt;

import com.alibaba.fastjson.JSONObject;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.Test;

public class MqttDemo {

    @Test
    public void testMqttPublish() {
        // MQTT服务器地址和端口
        String broker = "tcp://100.100.100.20:31017";
        // 客户端ID
        String clientId = "JavaDemoClient1";
        // 订阅的主题
        String topic = "/lwl_seq/type_test";
        // 消息质量服务等级
        int qos = 1;
        // 要发布的消息内容
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("int", 23);
        jsonObject.put("long", 23.233);
        jsonObject.put("float", 24.444);
        jsonObject.put("double", 25.555);
        jsonObject.put("string", "hello world");
        jsonObject.put("boolean", true);
        jsonObject.put("datetime", System.currentTimeMillis());
        String content = jsonObject.toString();

        try {
            // 创建一个MQTT异步客户端
            MqttAsyncClient client = new MqttAsyncClient(broker, clientId);
            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setAutomaticReconnect(true);

            // 设置回调，处理接收到的消息
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to MQTT broker lost!");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.printf("Message arrived. Topic: %s Message: %s%n", topic,
                            new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Delivery is complete!");
                }
            });

            // 连接到MQTT服务器
            System.out.println("Connecting to broker: " + broker);
            IMqttToken token = client.connect(options);
            token.waitForCompletion();
            if (token.isComplete() && token.getException() == null) {
                System.out.println("Connected with result code " + token.getResponse().toString());
            }

            // 发布消息
            for (int i = 0; i < 2; i++) {
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                System.out.println("Publishing message: " + content);
                client.publish(topic, message);
                Thread.sleep(1000);
            }
            // 等待一段时间后断开连接
            client.disconnect();
            client.close();
        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}