package com.supos.common.dto.protocol;

import lombok.Data;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.Min;

@Data
public class RateDTO {

    @Min(1)
    private long value;

    private String unit; // ms, s, m, h

    public String getUnit() {
        if (StringUtils.hasText(unit)) {
            return unit;
        }
        return "s";
    }

    public long getSeconds() {
        if (!StringUtils.hasText(unit)) {
            return 60; // 1分钟
        }
        switch(unit) {
            case "ms" : {
                long v = value / 1000;
                return v <= 0 ? 1 : v;
            }
            case "s": return value;
            case "m": return value * 60;
            case "h": return value * 3600;
            default: return 60;
        }
    }
}