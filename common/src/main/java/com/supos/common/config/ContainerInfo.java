package com.supos.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContainerInfo {

    private String name;

    private String version;

    private String description;

    /**
     * 环境变量
     */
    private Map<String,Object> envMap;
}
