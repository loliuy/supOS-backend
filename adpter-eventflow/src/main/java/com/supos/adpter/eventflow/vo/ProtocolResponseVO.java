package com.supos.adpter.eventflow.vo;

import com.supos.common.dto.protocol.KeyValuePair;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProtocolResponseVO {

    // 协议配置
    private List<FieldObject> clientConfig;

    // server配置
    private List<FieldObject> serverConfig;

    private Set<KeyValuePair<String>> outputDataType;

    private String serverConn;

}
