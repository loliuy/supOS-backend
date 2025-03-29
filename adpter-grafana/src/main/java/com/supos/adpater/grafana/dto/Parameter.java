package com.supos.adpater.grafana.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xinwangji@supos.com
 * @date 2024/10/12 10:24
 * @description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Parameter {

    /**
     * 字段名称
     */
    private String name;

    private String type = "functionParameter";
}
