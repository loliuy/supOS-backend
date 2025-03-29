package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.vo.FieldDefineVo;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelDetail {
    String topic;
    Integer dataType;// 1--时序库 2--关系库
    FieldDefineVo[] fields;// 字段定义
    Long createTime;// 创建时间--单位：毫秒

    String description;// 模型描述

    String alias;//模型别名


    String name;// 文件夹名称

    String modelId;//
    String modelName;//
}
