package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 系统模块
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum SysModuleEnum {

    /**
     * 模块
     */
    ALARM("system.module.alarm"),
    UNKNOWN("unknown")
    ;

    /**
     * 模板code
     */
    private String code;

    public static SysModuleEnum parse(String code) {
        for (SysModuleEnum each : values()) {
            if (code.equals(each.code)) {
                return each;
            }
        }
        return UNKNOWN;
    }

}

