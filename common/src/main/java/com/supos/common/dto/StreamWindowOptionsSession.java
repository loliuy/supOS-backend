package com.supos.common.dto;

import com.supos.common.Constants;
import com.supos.common.annotation.StreamTimeValidator;
import com.supos.common.enums.StreamWindowType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class StreamWindowOptionsSession {
    @NotEmpty
    @StreamTimeValidator(field = "tolValue")
    String tolValue;

    public String toString() {
        StringBuilder s = new StringBuilder(64 + tolValue.length());
        s.append(StreamWindowType.SESSION.name()).append('(')
                .append(Constants.SYS_FIELD_CREATE_TIME).append(',').append(tolValue).append(')');
        return s.toString();
    }
}
