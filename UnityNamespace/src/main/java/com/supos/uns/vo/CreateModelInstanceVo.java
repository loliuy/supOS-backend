package com.supos.uns.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.annotation.DataTypeValidator;
import com.supos.common.annotation.StreamTimeValidator;
import com.supos.common.annotation.TopicNameValidator;
import com.supos.common.dto.InstanceField;
import com.supos.common.dto.StreamOptions;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.JsonUtil;
import com.supos.common.vo.FieldDefineVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Data
@NoArgsConstructor
public class CreateModelInstanceVo {
    @Schema(description = "是否创建Flow")
    Boolean addFlow;
    @Schema(description = "是否创建数据看板")
    Boolean addDashBoard;
    @Schema(description = "是否创持久化")
    Boolean save2db = true;

    @Schema(description = "文件夹/文件名称")
    @TopicNameValidator
    @NotEmpty String topic;

    @Schema(description = "引用的其他文件主题")
    @TopicNameValidator
    String referTopic;//引用的其他实例主题

    @Schema(description = "1--时序库 2--关系库")
    @DataTypeValidator
    Integer dataType;// 1--时序库 2--关系库

    @Schema(description = "数据类型：1--时序，2--关系，3--计算型, 5--告警")
    String dataPath;

    @Schema(description = "字段定义")
    @Valid
    FieldDefineVo[] fields;// 模型字段定义，创建实例时可能指定实例的字段 Index
    @Schema(description = "文件夹描述")
    String modelDescription;// 文件夹描述
    @Schema(description = "文件描述")
    String instanceDescription;// 实例描述
    @Schema(description = "文件的协议")
    Map<String, Object> protocol;// 实例的协议
    /**
     * 文件夹、文件别名
     */
    @Schema(description = "文件夹、文件别名")
    @AliasValidator
    String alias;
    @Valid
    @Schema(description = "计算实例引用的其他文件字段")
    InstanceField[] refers;// 计算实例引用的其他文件字段
    @Size(max = 255)
    @Schema(description = "计算表达式")
    String expression;// 计算表达式
    @Valid
    @Schema(description = "流（历史）计算定义")
    StreamOptions streamOptions;// 流（历史）计算定义
    @Schema(description = "计算时间间隔")
    @StreamTimeValidator(field = "frequency")
    String frequency;// 计算时间间隔
    @Schema(description = "计算时间间隔")
    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    Long frequencySeconds;
    @Schema(description = "引用的其他多个文件主题")
    String[] referTopics;// 引用的其他多个文件主题
    @Schema(description = "模板id")
    String modelId;// 模板id

    public void setFrequency(String frequency) {
        this.frequency = frequency;
        if (frequency != null && !(frequency = frequency.trim()).isEmpty()) {
            Long nano = TimeUnits.toNanoSecond(frequency);
            if (nano != null) {
                frequencySeconds = nano / TimeUnits.Second.toNanoSecond(1);
            }
        }
    }
    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }

    public boolean isAddFlow() {
        return addFlow != null ? addFlow : false;
    }

    public boolean isAddDashBoard() {
        return addDashBoard != null ? addDashBoard : false;
    }
}
