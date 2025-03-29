package com.supos.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeUtils {
    static final ZoneId utcZone = Clock.systemUTC().getZone();
    static final DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    public static String getDateTimeStr(Object inTime) {
        String nowStr;
        if (inTime != null) {
            long dataInTimeMirco = (Long) inTime;
            Instant instant;
            if (dataInTimeMirco > 100000000000000000L) {// 微妙
                long dataInTimeMills = dataInTimeMirco / 1000000;
                int micro = (int) (dataInTimeMirco % 1000000);
                instant = Instant.ofEpochMilli(dataInTimeMills).plus(micro, ChronoUnit.MICROS);
            } else {// 毫秒
                instant = Instant.ofEpochMilli(dataInTimeMirco);
            }
            nowStr = instant.atZone(utcZone).format(fmt);
        } else {
            nowStr = ZonedDateTime.now(utcZone).format(fmt);
        }
        return nowStr;
    }

    public static long convertToMills(long timestamp) {
        if (timestamp > 10000000000000L) {
            timestamp = Long.parseLong(String.valueOf(timestamp).substring(0, 13));
        } else if (timestamp < 1000000000000L) {
            StringBuilder sr = new StringBuilder(16).append(timestamp);
            while (sr.length() < 13) {
                sr.append('0');
            }
            timestamp = Long.parseLong(sr.toString());
        }
        return timestamp;
    }
}
