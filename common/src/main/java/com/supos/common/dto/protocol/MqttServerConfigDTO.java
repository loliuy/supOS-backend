package com.supos.common.dto.protocol;

import lombok.Data;

@Data
public class MqttServerConfigDTO extends BaseServerConfigDTO {

    private String username;

    private String password;
}
