package com.supos.common.dto;

import com.supos.common.annotation.StreamTimeValidator;
import com.supos.common.enums.StreamWindowType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class StreamWindowOptionsInterval {
    @NotEmpty
    @StreamTimeValidator(field = "intervalValue")
    String intervalValue;

    @StreamTimeValidator(field = "intervalOffset")
    String intervalOffset;

    public String toString() {
        StringBuilder s = new StringBuilder(64 + intervalValue.length());
        s.append(StreamWindowType.INTERVAL.name()).append('(').append(intervalValue);
        if (intervalOffset != null) {
            s.append(',').append(intervalOffset);
        }
        s.append(')');
        return s.toString();
    }
}
