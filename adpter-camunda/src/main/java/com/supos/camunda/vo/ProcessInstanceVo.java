package com.supos.camunda.vo;

import com.supos.camunda.dto.NodeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程实例
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessInstanceVo {

    private Long processId;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;


    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 节点信息
     */
    private NodeDto node;


}
