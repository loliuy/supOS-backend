package com.supos.common.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supos.common.enums.StreamWindowType;
import lombok.Data;

import java.util.Map;

@Data
public class StreamWindowOptions {

    String windowType;
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    transient StreamWindowType streamWindowType;

    Map<String, Object> options;
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    transient Object optionBean;
}
