package com.supos.common.dto;

import com.supos.common.enums.StreamWindowType;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class StreamWindowOptionsCountWindow {

    @Min(2) @Max(Integer.MAX_VALUE)
    int countValue;

    Integer slidingValue;

    public String toString() {
        StringBuilder s = new StringBuilder(64);
        s.append(StreamWindowType.COUNT_WINDOW.name()).append('(').append(countValue);
        if (slidingValue != null) {
            s.append(',').append(slidingValue);
        }
        s.append(')');
        return s.toString();
    }
}
