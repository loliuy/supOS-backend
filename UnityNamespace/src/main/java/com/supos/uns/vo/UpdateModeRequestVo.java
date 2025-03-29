package com.supos.uns.vo;

import com.supos.common.vo.FieldDefineVo;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 修改模型的属性，只支持新增和修改
 * 修改模型描述
 */
@Data
@NoArgsConstructor
public class UpdateModeRequestVo {

    @NotNull
    String alias;

    FieldDefineVo[] fields;// 新增的字段

    String modelDescription;// 模型描述

}
