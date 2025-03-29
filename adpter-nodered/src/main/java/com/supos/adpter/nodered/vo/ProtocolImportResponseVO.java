package com.supos.adpter.nodered.vo;

import lombok.Data;

import java.util.List;

/**
 * 前端导入协议配置之后返回给前端的响应结果
 */
@Data
public class ProtocolImportResponseVO {
    // 客户端节点配置
    private List<FieldObject> clientConfig;

    // 服务端节点配置
    private List<FieldObject> serverConfig;

    // 客户端连接服务端的配置字段
    private String serverConn;

//    private Set<KeyValuePair<String>> outputDataTypes;

}
