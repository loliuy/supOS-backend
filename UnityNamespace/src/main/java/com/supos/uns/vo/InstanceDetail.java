package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.InstanceField;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.po.UnsLabelPo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceDetail {
    @Schema(description = "文件ID")
    String id;

    @Schema(description = "路径")
    String topic;

    @Schema(description = "1--时序库 2--关系库")
    Integer dataType;// 1--时序库 2--关系库

    @Schema(description = "0--保留（模板），1--时序，2--关系，3--计算型, 5--告警")
    String dataPath;

    @Schema(description = "字段定义")
    FieldDefineVo[] fields;// 字段定义

    @Schema(description = "创建时间--单位：毫秒")
    Long createTime;// 创建时间--单位：毫秒

    @Schema(description = "协议")
    Map<String, Object> protocol;

    @Schema(description = "模型描述")
    String modelDescription;// 模型描述

    @Schema(description = "实例描述")
    String instanceDescription;// 实例描述

    @Schema(description = "是否创建FLOW")
    boolean withFlow;

    @Schema(description = "是否创建看板")
    boolean withDashboard;

    @Schema(description = "是否持久化")
    boolean withSave2db;

    @Schema(description = "别名")
    String alias;// 别名

    @Schema(description = "表达式")
    String expression;

    @Schema(description = "引用对象")
    InstanceField[] refers;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表")
    List<UnsLabelPo> labelList;

    @Schema(description = "文件名")
    String name;//文件名

    String modelId;//

    String modelName;//
}
