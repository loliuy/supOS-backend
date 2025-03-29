package com.supos.common.utils;

import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.FieldType;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/3/3 11:12
 */
@Slf4j
public class FieldUtils {

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    private static final String SYS_FIELD_CREATE_TIME = Constants.SYS_FIELD_CREATE_TIME;
    private static final String SYS_FIELD_ID = Constants.SYSTEM_FIELD_PREV + "id";
    private static final Set<String> systemFields = new HashSet<>(Arrays.asList(SYS_FIELD_ID, SYS_FIELD_CREATE_TIME, Constants.QOS_FIELD));

    public static FieldDefine[] processFieldDefines(final String PATH, final SrcJdbcType jdbcType, FieldDefine[] fields, String[] err, boolean checkSysField) {
        HashMap<String, FieldDefine> fieldMap = new HashMap<>();
        for (FieldDefine f : fields) {
            final String name = f.getName();
            if (fieldMap.put(name, f) != null) {
                err[0] = I18nUtils.getMessage("uns.field.duplicate", name);
                return null;
            } else if (name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                if (checkSysField && !systemFields.contains(name)) {
                    err[0] = I18nUtils.getMessage("uns.field.startWith.limit.underline", name);
                    return null;
                }
            } else if (Character.isDigit(name.charAt(0))) {
                err[0] = I18nUtils.getMessage("uns.field.startWith.limit.number", name);
                return null;
            }

            if (f.getType() == FieldType.STRING && f.getMaxLen() == null) {
                String low = name.toLowerCase();
                f.setMaxLen(low.contains("name") || low.contains("tag") ? 64 : 250);
            }
        }
        FieldDefine createTimeField = fieldMap.get(SYS_FIELD_CREATE_TIME);
        if (createTimeField != null && FieldType.DATETIME != createTimeField.getType()) {
            err[0] = I18nUtils.getMessage("uns.field.type.must.be.datetime");
            return null;
        }
        if (jdbcType.typeCode == Constants.TIME_SEQUENCE_TYPE) {
            if (createTimeField == null) {
                FieldDefine[] newFs = new FieldDefine[3 + fields.length];
                newFs[0] = new FieldDefine(SYS_FIELD_CREATE_TIME, FieldType.DATETIME, true);//自动加上 ct 字段并作为主键
                newFs[newFs.length - 2] = new FieldDefine(Constants.QOS_FIELD, FieldType.LONG);//自动加上 qos 字段并作为质量码
                newFs[newFs.length - 1] = new FieldDefine(Constants.SYS_SAVE_TIME, FieldType.DATETIME);//自动加上 _st 字段并作为保存时间
                System.arraycopy(fields, 0, newFs, 1, fields.length);
                fields = newFs;
                log.debug("时序库类型自动加 _ct & _qos & _sct: {}", PATH);
            }
        } else {
            FieldDefine idField = fieldMap.get(SYS_FIELD_ID);
            boolean hasId = idField != null;
            if (!hasId) {
                for (FieldDefine f : fields) {
                    if (f.isUnique()) {
                        hasId = true;
                        break;
                    }
                }
            } else if (FieldType.LONG != idField.getType()) {
                err[0] = I18nUtils.getMessage("uns.field.type.must.be.long");
                return null;
            }
            if (!hasId || createTimeField == null) {
                FieldDefine[] newFs;
                if (!hasId && createTimeField == null) {
                    newFs = new FieldDefine[2 + fields.length];
                    newFs[0] = new FieldDefine(SYS_FIELD_ID, FieldType.LONG, true);//自动加上 id字段并作为主键
                    newFs[newFs.length - 1] = new FieldDefine(SYS_FIELD_CREATE_TIME, FieldType.DATETIME, false);
                    System.arraycopy(fields, 0, newFs, 1, fields.length);
                    log.debug("关系库类型自动加Id+ct: {}.{}", PATH, newFs[0].getName());
                } else if (!hasId) {
                    newFs = new FieldDefine[1 + fields.length];
                    newFs[0] = new FieldDefine(SYS_FIELD_ID, FieldType.LONG, true);
                    System.arraycopy(fields, 0, newFs, 1, fields.length);
                } else {
                    newFs = new FieldDefine[fields.length + 1];
                    newFs[newFs.length - 1] = new FieldDefine(SYS_FIELD_CREATE_TIME, FieldType.DATETIME, false);
                    System.arraycopy(fields, 0, newFs, 0, fields.length);
                }
                fields = newFs;
            }
        }
        return fields;
    }

    public static String validateFields(FieldDefine[] fields, boolean checkSysField) {
        HashMap<String, FieldDefine> fieldMap = new HashMap<>();
        for (FieldDefine f : fields) {
            final String name = f.getName();
            if (fieldMap.put(name, f) != null) {
                return I18nUtils.getMessage("uns.field.duplicate", name);
            }
            if (name.startsWith(Constants.SYSTEM_FIELD_PREV)) {
                if (checkSysField && !systemFields.contains(name)) {
                    return I18nUtils.getMessage("uns.field.startWith.limit.underline", name);
                }
                continue;
            }

            if (!FIELD_NAME_PATTERN.matcher(name).matches()) {
                return I18nUtils.getMessage("uns.field.name.format.invalid", name);
            }
        }

        FieldDefine createTimeField = fieldMap.get(SYS_FIELD_CREATE_TIME);
        if (createTimeField != null && FieldType.DATETIME != createTimeField.getType()) {
            return I18nUtils.getMessage("uns.field.type.must.be.datetime");
        }

        return null;
    }

}
