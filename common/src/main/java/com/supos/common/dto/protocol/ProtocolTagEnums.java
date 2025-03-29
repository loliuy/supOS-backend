package com.supos.common.dto.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

/**
 * 位号枚举
 */
@Data
@AllArgsConstructor
public class ProtocolTagEnums {

    private String name;

    private String dataType;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProtocolTagEnums that = (ProtocolTagEnums) o;
        return Objects.equals(name, that.name) && Objects.equals(dataType, that.dataType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dataType);
    }
}
