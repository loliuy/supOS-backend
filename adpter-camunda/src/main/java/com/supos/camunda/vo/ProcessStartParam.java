package com.supos.camunda.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/28 11:26
 */
@Data
public class ProcessStartParam {

    @NotBlank @Valid
    private Long processId;
    private Map<String, Object> variables;
}
