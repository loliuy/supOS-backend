package com.supos.adpter.nodered.vo;

import com.supos.common.dto.protocol.BaseServerConfigDTO;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class ProtocolTagsRequestVO {

    @NotEmpty
    private String protocolName;

    @NotEmpty
    private String serverConfig;

    private String topic;
}
