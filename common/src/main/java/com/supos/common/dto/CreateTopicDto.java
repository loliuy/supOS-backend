package com.supos.common.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import com.supos.common.SrcJdbcType;
import com.supos.common.annotation.DataTypeValidator;
import com.supos.common.annotation.StreamTimeValidator;
import com.supos.common.annotation.TopicNameValidator;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.JsonUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTopicDto {
    /**
     * 批次号
     */
    int batch;
    /**
     * 批次内序号
     */
    int index = -1;
    @NotEmpty
    @TopicNameValidator
    String topic;//主题

    @TopicNameValidator
    String referTopic;//引用的其他实例主题
    String referTable;
    FieldDefine[] refFields;// 流引用表的字段定义
    String referModelId; // 流引用表的 modeId
    @JsonIgnore
    String alias;
    String tableName;
    @DataTypeValidator
    Integer dataType;// 1--时序库 2--关系库
    SrcJdbcType dataSrcId;
    FieldDefine[] fields;// 字段定义

    String dataPath;// 数据在rest 数据当中的路径; 模型字段对应的数据字段映射放在 FieldDefine.index

    String description; //描述

    Map<String, Object> protocol;

    String protocolType;

    Object protocolBean;

    @Valid
    InstanceField[] refers;// 计算实例引用的其他实例字段
    @Size(max = 255)
    String expression;// 计算表达式

    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    @Expose(serialize = false, deserialize = false)
    transient Object compileExpression;

    @Valid
    StreamOptions streamOptions;// 流（历史）计算定义

    Integer flags;

    AlarmRuleDefine alarmRuleDefine;

    Boolean addFlow;
    Boolean addDashBoard;
    Boolean save2db;
    Boolean retainTableWhenDeleteInstance;

    @StreamTimeValidator(field = "frequency")
    String frequency;// 计算时间间隔
    @JsonIgnore
    @JSONField(deserialize = false, serialize = false)
    @com.alibaba.fastjson2.annotation.JSONField(deserialize = false, serialize = false)
    @Expose(serialize = false, deserialize = false)
    Long frequencySeconds;
    String[] referTopics;// 引用的其他多个实例主题

    String modelId;// 模板id
    String template;// 模板名称
    String extend;//扩展字段   workflow表主键ID
    String[] labelNames;//标签列表

    public void setFrequency(String frequency) {
        this.frequency = frequency;
        if (frequency != null && !(frequency = frequency.trim()).isEmpty()) {
            Long nano = TimeUnits.toNanoSecond(frequency);
            if (nano != null) {
                frequencySeconds = nano / TimeUnits.Second.toNanoSecond(1);
            }
        }
    }

    public String getTable() {
        if (tableName != null) {
            return tableName;
        }
        if (alias != null) {
            return alias;
        }
        return topic;
    }

    public CreateTopicDto(String topic, String alias) {
        setTopic(topic);
        this.alias = alias;
    }

    public CreateTopicDto(String topic, String alias, @NotEmpty FieldDefine[] fields) {
        setTopic(topic);
        this.fields = fields;
        this.alias = alias;
    }

    public CreateTopicDto(String topic, String alias, int dataType, @NotEmpty FieldDefine[] fields) {
        setTopic(topic);
        this.fields = fields;
        this.dataType = dataType;
        this.alias = alias;
    }

    public CreateTopicDto(String topic, String alias, Integer dataType, FieldDefine[] fields, String description) {
        setTopic(topic);
        this.dataType = dataType;
        this.fields = fields;
        this.description = description;
        this.alias = alias;
    }

    public CreateTopicDto(String topic, String alias, String description, Map<String, Object> protocol) {
        setTopic(topic);
        this.description = description;
        this.protocol = protocol;
        this.alias = alias;
    }

    public CreateTopicDto(String topic, String alias, String description, Map<String, Object> protocol, String protocolType) {
        setTopic(topic);
        this.description = description;
        this.protocol = protocol;
        this.alias = alias;
        this.protocolType = protocolType;
    }

    public CreateTopicDto setDataPath(String dataPath) {
        this.dataPath = dataPath;
        return this;
    }

    public CreateTopicDto setCalculation(InstanceField[] refers, String expression) {
        this.refers = refers;
        this.expression = expression;
        return this;
    }

    public CreateTopicDto setStreamCalculation(String referTopic, StreamOptions streamOptions) {
        this.referTopic = referTopic;
        this.streamOptions = streamOptions;
        return this;
    }

    public void setTopic(String topic) {
        if (topic != null && !topic.isEmpty()) {
            topic = topic.trim();
//            if (topic.charAt(0) == '/') {
//                topic = topic.substring(1);
//            }
        }
        this.topic = topic;
    }

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
