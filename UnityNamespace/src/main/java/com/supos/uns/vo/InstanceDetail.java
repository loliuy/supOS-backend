package com.supos.uns.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.dto.InstanceField;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.po.UnsLabelPo;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceDetail {
    String id;
    String topic;
    Integer dataType;// 1--时序库 2--关系库
    String dataPath;
    FieldDefineVo[] fields;// 字段定义
    Long createTime;// 创建时间--单位：毫秒

    Map<String, Object> protocol;
    String modelDescription;// 模型描述
    String instanceDescription;// 实例描述

    boolean withFlow;
    boolean withDashboard;
    boolean withSave2db;

    String alias;// 实例别名

    String expression;

    InstanceField[] refers;

    /**
     * 标签列表
     */
    List<UnsLabelPo> labelList;


    String name;//文件名

    String modelId;//
    String modelName;//
}
