package com.supos.uns.vo;

import com.supos.common.vo.FieldDefineVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
public class CreateTemplateVo {

    int batch;
    int index;

    /**
     * 模板名称
     */
    @NotEmpty(message = "The template name cannot be empty")
    @Schema(description = "模板名称")
    String path;
    /**
     * 字段定义
     */
    @NotNull(message = "The fields cannot be null")
    @Schema(description = "字段定义")
    FieldDefineVo[] fields;
    /**
     * 模板描述
     */
    @Schema(description = "模板描述")
    String description;

    public String gainBatchIndex() {
        return String.format("%d-%d", batch, index);
    }
}
