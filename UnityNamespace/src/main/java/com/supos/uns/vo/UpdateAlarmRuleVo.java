package com.supos.uns.vo;

import com.supos.common.utils.JsonUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class UpdateAlarmRuleVo extends CreateAlarmRuleVo {

    @NotEmpty
    String id;

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }
}
