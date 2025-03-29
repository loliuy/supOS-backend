package com.supos.uns.vo;

import com.supos.common.dto.JsonResult;
import lombok.Data;

public class RemoveResult extends JsonResult<RemoveResult.RemoveTip> {

    @Data
    public static class RemoveTip {
        Integer refs;
    }
}
