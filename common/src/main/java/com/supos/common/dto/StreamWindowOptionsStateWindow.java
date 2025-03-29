package com.supos.common.dto;

import com.supos.common.enums.StreamWindowType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class StreamWindowOptionsStateWindow {
    @NotEmpty
    String field;

    public String toString() {
        StringBuilder s = new StringBuilder(field.length() + 2);
        s.append(StreamWindowType.STATE_WINDOW.name()).append('(').append(field).append(')');
        return s.toString();
    }
}
