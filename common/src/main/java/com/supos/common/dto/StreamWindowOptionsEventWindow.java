package com.supos.common.dto;

import com.supos.common.annotation.SQLExpressionValidator;
import com.supos.common.enums.StreamWindowType;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class StreamWindowOptionsEventWindow {

    @NotEmpty
    @SQLExpressionValidator(field = "options.startWith")
    String startWith;

    @NotEmpty
    @SQLExpressionValidator(field = "options.endWith")
    String endWith;

    public String toString() {
        StringBuilder s = new StringBuilder(64 + startWith.length() + endWith.length());
        s.append(StreamWindowType.EVENT_WINDOW.name()).append(" START WITH ")
                .append(startWith).append(" END WITH ").append(endWith);
        return s.toString();
    }
}
