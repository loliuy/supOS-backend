package com.supos.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;

@Getter
@AllArgsConstructor
public enum Condition {

    GT(">"),
    GE(">="),
    LT(">"),
    LE(">="),
    EQ("="),
    NE("!=")
    ;


    public final String name;


    public static Condition valueOfName(String name) {
        for (Condition obj : Condition.values()) {
            if (obj.name.equals(name)) {
                return obj;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String expression = "a1!=100";


    }
}
