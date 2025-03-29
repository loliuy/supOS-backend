package com.supos.uns.vo;

import com.alibaba.fastjson.JSON;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.protocol.RestServerConfigDTO;
import com.supos.common.utils.JsonUtil;
import lombok.Data;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Valid
public class RestTestRequestVo {
    int timeoutConnectMills = 3000;//请求连接超时时间，单位：毫秒
    int timeoutReadMills = 9000;//请求读超时时间，单位：毫秒
    String method;// http方法，默认 GET
    String path;// uri 相对地址
    RestServerConfigDTO server;
    List<StrMapEntry> params;//Query参数
    PageDef pageDef;
    Map<String, Object> jsonBody;

    String fullUrl;// 完整地址不为空则忽略上面四个字段：host,port,address,params

    Map<String, Object> body;// Post 请求体，可为空

    String topic; // 实例topic
    FieldDefine[] fields;// 字段定义

    List<StrMapEntry> headers; // 请求头

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }

    public String getBody() {
        if (body == null) {
            return null;
        }
        return JSON.toJSONString(body);
    }

    public Map<String, String> getHeaderMap() {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        Map<String, String> headerMap = new HashMap<>();
        for (StrMapEntry header : headers) {
            headerMap.put(header.getKey(), header.getValue());
        }
        return headerMap;
    }
}
