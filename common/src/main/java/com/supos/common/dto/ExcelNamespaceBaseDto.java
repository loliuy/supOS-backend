package com.supos.common.dto;

import com.supos.common.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
public class ExcelNamespaceBaseDto {
    String topic;//主题
    Integer dataType;// 1--时序库 2--关系库
    String fields;// 字段定义

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
