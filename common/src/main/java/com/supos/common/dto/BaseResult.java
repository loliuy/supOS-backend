package com.supos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResult {
    int code;//错误码，0--正常，其他失败
    String msg;//错误信息
}
