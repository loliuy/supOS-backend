package com.supos.camunda.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 流程定义
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessDefinitionVo {

    private Long id;

    /**
     * 描述
     */
    private String description;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程名称
     */
    private String processDefinitionName;

    /**
     * 流程Key
     */
    private String processDefinitionKey;

    /**
     * 0草稿：未部署
     * 1运行：已部署，运行中
     * 2暂停：已部署，已暂停
     */
    private Integer status;

    /**
     * 部署ID
     */
    private String deployId;

    /**
     * 部署名称
     */
    private String deployName;

    /**
     * 部署时间
     */
    private Date deployTime;

    private String bpmnXml;

    /**
     * 实例列表
     */
    private List<ProcessInstanceVo> instanceList;

}
