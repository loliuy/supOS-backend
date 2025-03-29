package com.supos.common.dto.excel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.annotation.TopicNameValidator;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.apache.commons.lang3.StringUtils;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/2/20 19:11
 */
@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExcelNameSpaceDto {
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

    @AliasValidator
    String alias;//别名

    FieldDefine[] fields;// 字段定义

    String dataPath;// 数据在rest 数据当中的路径; 模型字段对应的数据字段映射放在 FieldDefine.index

    String description; //描述

    String template;// 模板名称

    public void setTopic(String topic) {
        if (topic != null && !topic.isEmpty()) {
            topic = topic.trim();
        }
        this.topic = topic;
    }

    public CreateTopicDto createTopic() {
        CreateTopicDto topicDto = new CreateTopicDto();
        topicDto.setIndex(index);
        topicDto.setBatch(batch);
        topicDto.setTopic(topic);
        topicDto.setAlias(StringUtils.isNotBlank(alias) ? alias : null);
        topicDto.setDescription(description);
        topicDto.setTemplate(template);
        topicDto.setDataPath(dataPath);
        return topicDto;
    }

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
