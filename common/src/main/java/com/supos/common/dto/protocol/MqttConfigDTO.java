package com.supos.common.dto.protocol;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class MqttConfigDTO extends BaseConfigDTO {

    private MqttServerConfigDTO server;

    private String serverName;

    private String inputName;

    private String inputTopic;

    public String getServerName() {
        if (StringUtils.hasText(serverName)) {
            return serverName;
        }
        return server.getHost() + ":" + server.getPort();
    }

    public String getInputName() {
        if (StringUtils.hasText(inputName)) {
            return inputName;
        }
        return "";
    }
}
