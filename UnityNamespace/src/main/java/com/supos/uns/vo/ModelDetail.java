package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.vo.FieldDefineVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelDetail {
    @Schema(description = "路径")
    String topic;
    @Schema(description = "1--时序库 2--关系库")
    Integer dataType;// 1--时序库 2--关系库
    @Schema(description = "字段定义")
    FieldDefineVo[] fields;// 字段定义
    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;// 创建时间--单位：毫秒
    @Schema(description = "模型描述")
    String description;// 模型描述
    @Schema(description = "模型别名")
    String alias;//模型别名

    @Schema(description = "文件夹名称")
    String name;// 文件夹名称
    String modelId;//
    String modelName;//
}
