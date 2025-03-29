package com.supos.adpter.nodered.dao.po;

import lombok.Data;

@Data
public class IOTProtocolPO {

    // 协议名称 全局唯一
    private String name;

    // 标记配置中哪个字段为连接server
    private String serverConn;

    private String description;

    private String clientConfigJson;

    private String serverConfigJson;
    // 1-自定义 0-系统自带
    private int custom;

}
