package com.supos.common.dto.mock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MockOpcuaDTO {

    private String variableName;

    private String dataType;

    private String variableRange;
}
