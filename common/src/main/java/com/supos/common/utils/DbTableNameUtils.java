package com.supos.common.utils;

public class DbTableNameUtils {

    public static String getFullTableName(String tableName) {
        if (tableName.charAt(0) == '"' && tableName.charAt(tableName.length() - 1) == '"') {
            return tableName;
        }
        StringBuilder sb = new StringBuilder(tableName.length() + 5);
        getFullTableName(tableName, sb);
        return sb.toString();
    }

    public static void getFullTableName(String tableName, StringBuilder sb) {
        if (tableName.charAt(0) == '"' && tableName.charAt(tableName.length() - 1) == '"') {
            sb.append(tableName);
            return;
        }
        int dot = tableName.indexOf('.');
        if (dot > 0) {
            String db = tableName.substring(0, dot);
            String table = tableName.substring(dot + 1);
            addQuotation(sb, db);
            sb.append('.');
            addQuotation(sb, table);
        } else {
            addQuotation(sb, tableName);
        }
    }

    public static String getCleanTableName(String tableName) {
        int st = Math.max(0, tableName.lastIndexOf('.') + 1), ed = tableName.length();
        if (tableName.charAt(st) == '"') {
            st++;
        }
        if (tableName.charAt(ed - 1) == '"') {
            ed--;
        }
        return tableName.substring(st, ed);
    }

    private static void addQuotation(StringBuilder sb, String db) {
        if (db.charAt(0) != '"') {
            sb.append('"');
        }
        sb.append(db);
        if (db.charAt(db.length() - 1) != '"') {
            sb.append('"');
        }
    }
}
