package com.supos.adpter.nodered.dao.po;

import lombok.Data;

import java.util.Date;

@Data
public class NodeFlowPO {

    private long id;

    private String flowId;

    private String flowName;

    /**
     * 流程json数据
     */
    private String flowData;

    /**
     * 流程状态  see #com.supos.adpter.nodered.enums.FlowStatus
     */
    private String flowStatus;

    /**
     * 流程模版来源
     */
    private String template;

    private String description;



    private Date createTime;

    private Date updateTime;
}
