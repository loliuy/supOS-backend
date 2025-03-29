package com.supos.camunda.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/28 13:01
 */
@Data
public class TaskSubmit {

    private String taskId;

    private String processInstanceId;

    private Map<String, Object> variables;

    private String userId;
}
