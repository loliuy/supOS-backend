package com.supos.common.dto.protocol;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class ModbusServerConfigDTO extends BaseServerConfigDTO {

    private String unitId;

}
