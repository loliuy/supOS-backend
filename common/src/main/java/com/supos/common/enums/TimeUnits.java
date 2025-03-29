package com.supos.common.enums;

import com.supos.common.utils.IntegerUtils;

import java.util.HashMap;

public enum TimeUnits {
    //时间单位： b（纳秒）、u（微秒）、a（毫秒）、s（秒）、m（分）、h（小时）、d（天）、w（周）
    NanoSecond('b', 1),
    MicroSecond('u', 1000),
    MillsSecond('a', 1000000),
    Second('s', 1000000000),
    Minutes('m', 60000000000L),
    Hours('h', 60000000000L * 60),
    Day('d', 60000000000L * 60 * 24),
    Week('w', 60000000000L * 60 * 24 * 7),
    ;
    public final char code;
    private final long multiple;

    TimeUnits(char code, long multiple) {
        this.code = code;
        this.multiple = multiple;
    }

    public static TimeUnits of(char code) {
        return unitsHashMap.get(code);
    }

    public static Long toNanoSecond(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.length() >= 2) {
            value = value.trim();
            Integer timeNum = IntegerUtils.parseInt(value.substring(0, value.length() - 1).trim());
            if (timeNum != null) {
                char unit = value.charAt(value.length() - 1);
                TimeUnits timeUnits = TimeUnits.of(unit);
                if (timeUnits != null) {
                    return timeUnits.toNanoSecond(timeNum);
                }
            }
        }
        return null;
    }

    public long toNanoSecond(long value) {
        return value * multiple;
    }

    private static final HashMap<Character, TimeUnits> unitsHashMap = new HashMap<>(8);

    static {
        for (TimeUnits unit : TimeUnits.values()) {
            unitsHashMap.put(unit.code, unit);
        }
    }
}
