package com.supos.uns.vo;

import com.supos.common.vo.FieldDefineVo;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "别名")
    String alias;

    @Schema(description = "新增的字段")
    FieldDefineVo[] fields;// 新增的字段

    @Schema(description = "文件夹描述")
    String modelDescription;// 模型描述

}
