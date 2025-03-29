package com.supos.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeDto {

    /**
     * 总结点数
     */
    private Integer totalNode;

    /**
     * 当前节点位置
     */
    private Integer currentIndex;

    /**
     * 当前节点名称
     */
    private String currentNodeName;
}
