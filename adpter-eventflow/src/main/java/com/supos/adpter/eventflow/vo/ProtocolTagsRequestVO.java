package com.supos.adpter.eventflow.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ProtocolTagsRequestVO {

    @NotEmpty
    private String protocolName;

    @NotEmpty
    private String serverConfig;

    private String topic;
}
