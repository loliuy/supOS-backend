package com.supos.adapter.mqtt.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
@Getter
@Configuration
public class MQTTConfig {
    @Value("${mqtt.broker:}")
    private String broker;
    @Value("${mqtt.clientId:supos_server}")
    private String clientIdPrefix = "supos_server";

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(broker)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                broker = "tcp://100.100.100.20:31017";
            } else {
                broker = "tcp://emqx:1883";
            }
        }
    }
}
