package com.supos.common.dto.protocol;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class BaseServerConfigDTO {

    @NotBlank
    private String host;

    private String port;


}
