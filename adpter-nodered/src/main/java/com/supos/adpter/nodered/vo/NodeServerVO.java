package com.supos.adpter.nodered.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class NodeServerVO implements Serializable {
    private static final long serialVersionUID = 1l;

    private String id;
    //
    private String serverName;

    // 协议名称 see com.supos.common.enums.IOTProtocol
    private String protocolName;

    // 服务端配置
    private Map<String, Object> server;
}
