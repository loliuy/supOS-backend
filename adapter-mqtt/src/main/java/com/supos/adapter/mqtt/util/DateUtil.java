package com.supos.adapter.mqtt.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

    public static String dateStr(Long mills) {
        if (mills == null) {
            return null;
        }
        Instant instant = Instant.ofEpochMilli(mills);
        return instant.atOffset(ZoneOffset.ofHours(8)).format(fmt);
    }
}
