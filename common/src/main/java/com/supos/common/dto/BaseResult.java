package com.supos.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResult {
    @Schema(description = "错误码，0--正常，其他失败")
    int code;//错误码，0--正常，其他失败
    @Schema(description = "错误信息")
    String msg;//错误信息
}
