package com.supos.adpter.tdengine;

import com.alibaba.fastjson.JSON;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public class TdJdbcTest {

    @Test
    public void testSql() {
        String sql = "CREATE     STREAM   \n test   \t    INTO /company/fx/department/it/stream/test AS\n" +
                "SELECT ts, count(*), avg(value) FROM /company/fx/department/it INTERVAL(1m) SLIDING(30s);";
        String[] segments = sql.split("\\s+");
        for (int i = 0; i < segments.length; i++) {
            System.out.printf("%d: %s\n", i, segments[i]);
        }
        for (char c : "a \n \t \r\n".toCharArray()) {
            System.out.println(Character.isWhitespace(c));
        }
    }

    @Test
    public void testDateFmt() throws InterruptedException {
//        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
//        System.out.println(now);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
        Clock utcClock = Clock.systemUTC();
//
//        now = ZonedDateTime.now(utcClock).format(fmt);
//        System.out.println(now);
////        Thread.sleep(1000);
//        now = ZonedDateTime.now(utcClock).format(fmt);
//        System.out.println(now);
//
//        System.out.println(System.currentTimeMillis());
//        System.out.println(System.nanoTime());

        {
            long dataInTimeMirco;
            {
                Instant nowInstant = Instant.now();
                long now = nowInstant.toEpochMilli();
                int micro = nowInstant.get(ChronoField.MICRO_OF_SECOND);
                dataInTimeMirco = now * 100_0000 + micro;// 精确到微秒
            }
            Instant instant;
            if (dataInTimeMirco > 100000000000000000L) {// 微妙
                long dataInTimeMills = dataInTimeMirco / 1000000;
                int micro = (int) (dataInTimeMirco % 1000000);
                System.out.println("dataInTimeMills = " + dataInTimeMills + ", micro=" + micro);
                instant = Instant.ofEpochMilli(dataInTimeMills).plus(micro, ChronoUnit.MICROS);
            } else {// 毫秒
                instant = Instant.ofEpochMilli(dataInTimeMirco);
            }
            String nowStr = instant.atOffset(ZoneOffset.UTC).format(fmt);
            System.out.println("nowStr: " + nowStr);
        }

        // 获取当前时间的毫秒表示
        long currentTimeMillis = System.currentTimeMillis();
        Instant nowInstant = Instant.now();
        long nowMills = nowInstant.toEpochMilli();
        int micro = nowInstant.get(ChronoField.MICRO_OF_SECOND);
        System.out.println("currentTimeMillis: " + currentTimeMillis);
        System.out.println("InstantNow Millis: " + nowMills + ", micro = " + micro);
        // 将当前时间转换为即时对象
        Instant instant = Instant.ofEpochMilli(currentTimeMillis);// .with(ChronoField.NANO_OF_SECOND, nanoTime);
        String now = instant.atOffset(ZoneOffset.UTC).format(fmt);

        System.out.println("ZonedDateTime1: " + now);
        System.out.println("ZonedDateTime2: " + instant.plus(micro, ChronoUnit.MICROS).atOffset(ZoneOffset.UTC).format(fmt));

    }

    @Test
    public void testTdConnect() throws Exception {
        HikariConfig config = new HikariConfig();
        // jdbc properties
        //  config.setJdbcUrl("jdbc:TAOS://100.100.100.20:6030/log");
//        Class.forName("com.taosdata.jdbc.ws.WebSocketDriver");


        config.setJdbcUrl("jdbc:TAOS-WS://100.100.100.20:31016/"); //ws: 6041, rest: 6030
        config.setUsername("root");
        config.setPassword("taosdata");
        config.setCatalog("public");
        // connection pool configurations
        config.setMinimumIdle(10); // minimum number of idle connection
        config.setMaximumPoolSize(10); // maximum number of connection in the pool
        config.setConnectionTimeout(30000); // maximum wait milliseconds for get connection from pool
        config.setMaxLifetime(0); // maximum life time for each connectionT
        config.setIdleTimeout(0); // max idle time for recycle idle connection
        config.setConnectionTestQuery("SELECT SERVER_VERSION()"); // validation query

        HikariDataSource dataSource = new HikariDataSource(config); // create datasource

        try (Connection connection = dataSource.getConnection()) {
            // get connection
            try (Statement statement = connection.createStatement()) {
                statement.execute("create database if not exists public");
                statement.execute("CREATE TABLE if not exists public.dev_t5 (ts TIMESTAMP, speed INT, `tag` NCHAR(255) )");
                statement.execute("use public");
            }
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("show databases");
                while (rs.next()) {
                    System.out.println("** db: " + rs.getString(1));
                }
            }

            try (Statement statement = connection.createStatement()) {
                String db = "public", tableName = "_$alarm_7150236bcc72_9cd9644bc57842768de7";
                String sql = "DESCRIBE " + db + ".`" + tableName + "`";
                ResultSet rs = statement.executeQuery(sql);
                Map<String, String> fm = new LinkedHashMap<>();
                while (rs.next()) {
                    String col = rs.getString("field"), type = rs.getString("type");
                    Object len = rs.getObject("length");
                    fm.put(col, type + (len != null ? " " + len : ""));
                }
                System.out.println(JSON.toJSONString(fm, true));
            }

        }
    }
}
