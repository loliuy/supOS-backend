package com.supos.uns.vo;

import com.supos.common.utils.JsonUtil;
import lombok.Data;

import java.util.Collection;

@Data
public class RestTestResponseVo {
    String dataPath;
    Collection<String> dataFields;

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
