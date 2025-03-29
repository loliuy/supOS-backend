package com.supos.uns.vo;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class StreamNewVo {
    @NotEmpty
    String namespace;// 命名空间
    String description;// 描述
    @NotEmpty
    String sql;// 创建流的执行语句
}
