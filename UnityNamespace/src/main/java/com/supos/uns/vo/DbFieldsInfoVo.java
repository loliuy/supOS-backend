package com.supos.uns.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DbFieldsInfoVo {
    String databaseType;
    @NotEmpty(message = "uns.invalid.emptyFields")
    DbFieldDefineVo[] fields;

    public DbFieldsInfoVo(String databaseType, @NotEmpty(message = "uns.invalid.emptyFields") DbFieldDefineVo[] fields) {
        this.databaseType = databaseType;
        this.fields = fields;
    }
}
