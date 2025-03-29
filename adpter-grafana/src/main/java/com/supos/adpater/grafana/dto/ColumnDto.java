package com.supos.adpater.grafana.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * grafana dashboard column
 * @author xinwangji@supos.com
 * @date 2024/10/12 10:22
 * @description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnDto {

    private String type = "function";

    /**
     * 字段参数
     */
    private List<Parameter> parameters;


    public static ColumnDto newColumn(String name){
        ColumnDto column = new ColumnDto();
        Parameter parameter = new Parameter();
        parameter.setName(name);
        column.setParameters(Arrays.asList(parameter));
        return column;
    }
}
