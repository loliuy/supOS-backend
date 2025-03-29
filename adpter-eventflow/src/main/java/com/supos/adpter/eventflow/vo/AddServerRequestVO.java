package com.supos.adpter.eventflow.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Valid
public class AddServerRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;
    //
    @NotEmpty(message = "name can't be empty")
    private String serverName;

    // 协议名称 see com.supos.common.enums.IOTProtocol
    @NotEmpty(message = "protocol can't be empty")
    private String protocolName;

    // 服务端配置
    @NotEmpty(message = "config can't be empty")
    private Map<String, Object> server;
}
