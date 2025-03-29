package com.supos.common.dto.protocol;

import lombok.Data;

import jakarta.validation.constraints.Min;

@Data
public class ICMPConfigDTO extends BaseConfigDTO {

    @Min(1)
    private int interval; // ping时间间隔

    @Min(1)
    private int timeout; // 超时时间，单位秒

    @Min(1)
    private int retry; // 重试次数

    private BaseServerConfigDTO server;
}
