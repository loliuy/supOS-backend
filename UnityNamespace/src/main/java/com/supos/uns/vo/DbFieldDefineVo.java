package com.supos.uns.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DbFieldDefineVo {
    @NotEmpty(message = "uns.invalid.emptyFieldName")
    String name;
    @NotEmpty(message = "uns.invalid.emptyFieldType")
    String columnType;
    Integer columnSize;
    String comment;
}
