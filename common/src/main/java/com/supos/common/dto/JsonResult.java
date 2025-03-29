package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.utils.JsonUtil;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonResult<T> extends BaseResult {
    T data;//返回数据

    public JsonResult() {
    }

    public JsonResult(int code, String msg) {
        super(code, msg);
    }

    public JsonResult(int code, String msg, T data) {
        super(code, msg);
        this.data = data;
    }

    public JsonResult<T> setData(T data) {
        this.data = data;
        return this;
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
