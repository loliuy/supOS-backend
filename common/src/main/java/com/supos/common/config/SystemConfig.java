package com.supos.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "sys.os")
public class SystemConfig {

    private String appTitle;
    /**
     * 系统版本
     */
    private String version;

    /**
     * 语言
     * en-US
     * zh-CN
     */
    private String lang;

    /**
     * 是否开启keycloak校验
     */
    private Boolean authEnable = false;

    /**
     * 大语言模型
     */
    private String llmType = "ollama";

    /**
     * mqtt tcp端口
     */
    private Integer mqttTcpPort = 1883;

    /**
     * mqtt WebSocket加密端口
     */
    private Integer mqttWebsocketTslPort = 8084;

    /**
     * 登录页url
     * @法约
     */
    private String loginPath = "/supos-login";

    /**
     * 基础平台类型
     */
    private String platformType = "linux";

    /**
     * 系统入口地址：PROTOCOL+DOMAIN+PORT
     */
    private String entranceUrl;

    /**
     * 单双topic
     */
    private boolean multipleTopic;

    /**
     * 系统容器
     * key:容器名称
     * value:容器信息
     */
    Map<String,ContainerInfo> containerMap = new HashMap<>();
}
