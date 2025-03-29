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
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

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
public class ExcelFolderDto {

    /**
     * 批次号
     */
    int batch;
    /**
     * 批次内序号
     */
    int index = -1;

    @NotEmpty
    @TopicNameValidator(message = "uns.folder.format.invalid")
    String name;//主题

    @AliasValidator
    String alias;//别名

    FieldDefine[] fields;// 字段定义

    String description; //描述


    String template;// 模板名称

    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            name = name.trim();
            if (!StringUtils.endsWith(name, "/")) {
                name += "/";
            }
        }
        this.name = name;
    }

    public CreateTopicDto createTopic() {
        CreateTopicDto topicDto = new CreateTopicDto();
        topicDto.setIndex(index);
        topicDto.setBatch(batch);
        topicDto.setTopic(name);
        topicDto.setAlias(StringUtils.isNotBlank(alias) ? alias : null);
        topicDto.setDescription(description);
        topicDto.setTemplate(template);
        return topicDto;
    }

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
