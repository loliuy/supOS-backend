package com.supos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Data
@Valid
@NoArgsConstructor
@AllArgsConstructor
public class DropTableDto {
    @NotEmpty
    String topic;
    String srcType;// pg--PostgreSQL,td---TdEngine
}
