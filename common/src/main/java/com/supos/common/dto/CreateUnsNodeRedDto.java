package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.annotation.AliasValidator;
import com.supos.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateUnsNodeRedDto {

    @NotEmpty(message = "uns.name.empty")
    String name;// 文件名

    @Hidden
    String path;// 文件路径

    @AliasValidator
    @Schema(description = "别名")
    String alias;

    String fieldType;

    String fieldName;

    String tag;


    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
