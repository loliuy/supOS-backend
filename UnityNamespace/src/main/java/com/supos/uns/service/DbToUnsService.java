package com.supos.uns.service;

import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.JsonResult;
import com.supos.common.enums.FieldType;
import com.supos.common.utils.MariadbTypeUtils;
import com.supos.common.utils.PostgresqlTypeUtils;
import com.supos.common.utils.SqlserverTypeUtils;
import com.supos.uns.vo.DbFieldDefineVo;
import com.supos.uns.vo.DbFieldsInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.function.Function;

@Slf4j
@Service
public class DbToUnsService {

    public JsonResult<FieldDefine[]> parseDatabaseFields(DbFieldsInfoVo fs) {
        DbFieldDefineVo[] fields;
        if (fs == null || ArrayUtils.isEmpty(fields = fs.getFields())) {
            return new JsonResult<>();
        }
        Function<String, String> typeConverter = null;
        String databaseType = fs.getDatabaseType();
        if (databaseType != null) {
            String lower = databaseType.toLowerCase();
            if (lower.contains("mysql") || lower.contains("mariadb")) {
                typeConverter = MariadbTypeUtils.dbType2FieldTypeMap::get;
            } else if (lower.contains("sqlserver")) {
                typeConverter = SqlserverTypeUtils.dbType2FieldTypeMap::get;
            } else if (lower.contains("postgres")) {
                typeConverter = PostgresqlTypeUtils.dbType2FieldTypeMap::get;
            }
        }
        if (typeConverter == null) {
            typeConverter = k -> {
                String v = PostgresqlTypeUtils.dbType2FieldTypeMap.get(k);
                if (v == null) {
                    v = SqlserverTypeUtils.dbType2FieldTypeMap.get(k);
                }
                if (v == null) {
                    v = MariadbTypeUtils.dbType2FieldTypeMap.get(k);
                }
                return v;
            };
        }
        ArrayList<FieldDefine> list = new ArrayList<>(fields.length);
        for (DbFieldDefineVo vo : fields) {
            String type = vo.getColumnType();
            if (type != null) {
                type = type.toLowerCase();
            }
            String typeName = typeConverter.apply(type);
            FieldType fieldType;
            if (typeName != null && (fieldType = FieldType.getByName(typeName)) != null) {
                FieldDefine define = new FieldDefine(vo.getName(), fieldType);
                define.setMaxLen(vo.getColumnSize());
                define.setRemark(vo.getComment());
                list.add(define);
            } else {
                log.debug("FieldType404: {} {}", type, vo.getName());
            }
        }
        return new JsonResult<FieldDefine[]>().setData(list.toArray(new FieldDefine[0]));
    }
}
