package com.supos.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.HashMap;

@Getter
public enum FieldType {

    INT("int", true),

    LONG("long", true),

    FLOAT("float", true),

    DOUBLE("double", true),

    BOOLEAN("boolean", false),

    DATETIME("datetime", false),
    
    STRING("string", false),
    ;

    public final String name;
    public final boolean isNumber;

    FieldType(String name, boolean isNumber) {
        this.name = name;
        this.isNumber = isNumber;
    }

    private static final HashMap<String, FieldType> nameMap = new HashMap<>(16);

    static {
        for (FieldType ft : FieldType.values()) {
            nameMap.put(ft.name, ft);
        }
    }

    public static FieldType getByName(String name) {
        return nameMap.get(name);
    }

    @JsonCreator
    public static FieldType getByNameIgnoreCase(String name) {
        return getByName(name.toLowerCase());
    }

    public String toString() {
        return name;
    }

}
