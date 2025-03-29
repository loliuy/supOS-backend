package com.supos.uns.bo;

import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: 实例拓扑事件数据
 * @date 2024/12/20 9:23
 */
@Data
public class InstanceTopologyData {

    /**
     * 事件节点
     */
    String topologyNode;

    /**
     * 事件编码
     */
    String eventCode;

    /**
     * 事件详情
     */
    String eventMessage;

    /**
     * 事件发生时间
     */
    Long eventTime;
}
