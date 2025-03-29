package com.supos.common.enums;

import lombok.Getter;

@Getter
public enum IOTDataType {

    BASIC("basic"),

    ARRAY("array"),

    JSON("key/value(json)");

    private String desc;

    IOTDataType(String desc) {
        this.desc = desc;
    }

}
