package com.supos.adpter.pg;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.IdUtil;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.DataStorageAdapter;
import com.supos.common.annotation.Description;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.SaveDataEvent;
import com.supos.common.utils.DbTableNameUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PostgresqlTypeUtils;
import com.supos.common.vo.FieldDefineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

import static com.supos.common.utils.DateTimeUtils.getDateTimeStr;

@Slf4j
public class PostgresqlEventHandler extends PostgresqlBase implements DataStorageAdapter {

    public PostgresqlEventHandler(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public String name() {
        return "PostgresqlAdapter";
    }

    @Override
    public SrcJdbcType getJdbcType() {
        return SrcJdbcType.Postgresql;
    }

    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(9)
    @Description("uns.create.task.name.pg")
    void onBatchCreateTableEvent(BatchCreateTableEvent event) {
        CreateTopicDto[] topics = event.topics.get(SrcJdbcType.Postgresql);
        if (topics != null && event.getSource() != this) {
            super.doTx(() -> batchCreateTables(topics));
        }
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(9)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        if (event.jdbcType != SrcJdbcType.Postgresql) {
            return;
        }
        List<String> sqls = Collections.EMPTY_LIST;
        Collection<String> tables = event.topics.values().stream().filter(ins -> ins.isRemoveTableWhenDeleteInstance())
                .map(in -> in.getTableName()).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(tables)) {
            sqls = new ArrayList<>(tables.size());
            for (String table : tables) {
                sqls.add("drop table if exists " + DbTableNameUtils.getFullTableName(table));
            }
        }
        if (sqls.size() > 0) {
            log.debug("PostGreSQL 删除：{}", sqls);
            jdbcTemplate.batchUpdate(sqls.toArray(new String[0]));
        }
    }

    private String[] getDbAndTable(String tableName) {
        int dot = tableName.indexOf('.');
        String dbName = this.currentSchema;
        if (dot > 0) {
            dbName = tableName.substring(0, dot);
            tableName = tableName.substring(dot + 1);
        }
        return new String[]{dbName, tableName};
    }

    @EventListener(classes = SaveDataEvent.class)
    @Order(9)
    void onSaveData(SaveDataEvent event) {
        if (SrcJdbcType.Postgresql == event.jdbcType && event.getSource() != this) {
            ArrayList<Pair<SaveDataDto, String>> SQLs = new ArrayList<>(2 * event.topicData.length);
            for (SaveDataDto dto : event.topicData) {
                String table = DbTableNameUtils.getFullTableName(dto.getTable());
                for (List<Map<String, Object>> list : Lists.partition(dto.getList(), Constants.SQL_BATCH_SIZE)) {
                    String insertSQL = getInsertSQL(list, table, dto.getFieldDefines());
                    SQLs.add(Pair.of(dto, insertSQL));
                }
            }
            List<List<Pair<SaveDataDto, String>>> segments = Lists.partition(SQLs, Constants.SQL_BATCH_SIZE);
            for (List<Pair<SaveDataDto, String>> sqlPairList : segments) {
                List<String> sqlList = sqlPairList.stream().map(Pair::getValue).collect(Collectors.toList());
                log.debug("PgWrite: \n{}", sqlList);
                String[] sqlArray = sqlList.toArray(new String[0]);
                try {
                    jdbcTemplate.batchUpdate(sqlArray);
                } catch (Exception ex) {
                    Set<String> topics = sqlPairList.stream().map(s -> s.getKey().getTopic()).collect(Collectors.toSet());
                    TopologyLog.log(topics, TopologyLog.Node.DATA_PERSISTENCE, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.db.pg"));
                    CreateTopicDto[] dtos = sqlPairList.stream().map(s -> s.getKey().getCreateTopicDto())
                            .toArray(CreateTopicDto[]::new);
                    log.error("PG写入失败: {}, {}", ex.getMessage(), sqlList);
                    try {
                        batchCreateTables(dtos);
                        jdbcTemplate.batchUpdate(sqlArray);
                        log.debug("retry success!");
                    } catch (Exception rex) {
                        log.error("PG写入 re失败:" + topics, rex);
                    }
                }
            }
        }
    }

    void batchCreateTables(CreateTopicDto[] topics) {
        // 批量执行 postgresql 建表
        List<String> createTableSQLs = new ArrayList<>(topics.length);
        for (CreateTopicDto dto : topics) {
            String tableName = dto.getTable();
            String quotationTableName = DbTableNameUtils.getFullTableName(tableName);
            String createTableSQL = getCreateTableSQL(quotationTableName, dto.getFields());

            int dot = tableName.indexOf('.');
            String dbName = this.currentSchema;
            if (dot > 0) {
                dbName = tableName.substring(0, dot);
                tableName = tableName.substring(dot + 1);
            }
            Map<String, String> oldFieldTypes = fieldTypes(dbName, tableName);
            if (!oldFieldTypes.isEmpty()) {
                Map<String, FieldDefine> curFieldTypes = Arrays.stream(dto.getFields()).collect(Collectors.toMap(FieldDefine::getName, d -> d));

                boolean hasTypeChanged = false;
                LinkedList<String> delFs = new LinkedList<>();
                for (Map.Entry<String, String> entry : oldFieldTypes.entrySet()) {
                    String field = entry.getKey(), oldType = entry.getValue();
                    FieldDefine curType = curFieldTypes.remove(field);
                    if (curType == null) {
                        delFs.add(field);
                    } else if (oldType != null && !curType.getType().name.equals(oldType)) {
                        hasTypeChanged = true;
                        break;
                    }
                }
                if (hasTypeChanged) {// 修改字段类型的情况则删除表
                    String dropSQL = "drop table IF EXISTS \"" + dbName + "\".\"" + tableName + '"';
                    createTableSQLs.add(dropSQL);
                } else if (!delFs.isEmpty() || !curFieldTypes.isEmpty()) {
                    // pg 删除或新增字段
                    final StringBuilder alterSQL = new StringBuilder(128)
                            .append("ALTER TABLE \"")
                            .append(dbName).append("\".\"").append(tableName).append('"');
                    for (String delF : delFs) {
                        alterSQL.append(" DROP IF EXISTS \"").append(delF).append("\",");
                    }
                    for (Map.Entry<String, FieldDefine> entry : curFieldTypes.entrySet()) {
                        FieldDefine def = entry.getValue();
                        String field = entry.getKey(), type = fieldType2DBTypeMap.get(def.getType().name);
                        type = getStringType(def, type);
                        alterSQL.append(" ADD IF NOT EXISTS \"").append(field).append("\" ").append(type).append(",");
                    }
                    createTableSQLs.add(alterSQL.substring(0, alterSQL.length() - 1));
                    continue;
                }
            }
            createTableSQLs.add(createTableSQL);
        }
        log.debug("PgCreateTable: {} {}", createTableSQLs.size(), createTableSQLs);
        List<List<String>> segments = Lists.partition(createTableSQLs, Constants.SQL_BATCH_SIZE);
        for (List<String> sqlList : segments) {
            try {
                jdbcTemplate.batchUpdate(sqlList.toArray(new String[0]));
            } catch (Exception ex) {
                log.error("PgCreateTable Error: " + sqlList, ex);
                throw ex;
            }
        }
    }

    private static String getStringType(FieldDefine def, String type) {
        Integer len = def.getMaxLen();
        if (len != null && def.getType() == FieldType.STRING) {
            type = "VARCHAR(" + len + ")";
        }
        return type;
    }

    Map<String, String> fieldTypes(String db, String tableName) {
        String sql = "SELECT column_name, udt_name FROM information_schema.columns WHERE table_name = ? and table_schema = ?";
        return jdbcTemplate.query(sql, new Object[]{tableName, db}, new int[]{Types.VARCHAR, Types.VARCHAR}, rs -> {
            Map<String, String> fm = new LinkedHashMap<>();
            while (rs.next()) {
                String col = rs.getString(1), type = rs.getString(2);
                String fieldType = PostgresqlTypeUtils.dbType2FieldTypeMap.get(type.toLowerCase());
                fm.put(col, fieldType);
            }
            return fm;
        });
    }

    private static final Map<Class, String> javaTypeToPgTypeMap = new HashMap<>(16);
    private static final HashSet<String> pgTypeMap;

    static {
        javaTypeToPgTypeMap.put(Integer.class, "integer");
        javaTypeToPgTypeMap.put(Long.class, "bigint");
        javaTypeToPgTypeMap.put(Double.class, "float");
        javaTypeToPgTypeMap.put(Boolean.class, "boolean");
        javaTypeToPgTypeMap.put(String.class, "text");
        pgTypeMap = new HashSet<>(javaTypeToPgTypeMap.values());

    }

    static final Map<String, String> fieldType2DBTypeMap = new HashMap<>(8);

    static {
        // {"int", "long", "float", "string", "boolean", "datetime"}
        fieldType2DBTypeMap.put(FieldType.INT.name, "int4");
        fieldType2DBTypeMap.put(FieldType.LONG.name, "int8");
        fieldType2DBTypeMap.put(FieldType.FLOAT.name, "float4");
        fieldType2DBTypeMap.put(FieldType.DOUBLE.name, "float8");
        fieldType2DBTypeMap.put(FieldType.STRING.name, "text");
        fieldType2DBTypeMap.put(FieldType.BOOLEAN.name, "boolean");
        fieldType2DBTypeMap.put(FieldType.DATETIME.name, "timestamptz");
    }


    static String getInsertSQL(Collection<Map<String, Object>> list, String table, FieldDefines defines) {
        /**
         * INSERT INTO test (id, name, email)
         * VALUES (1, 'Alice', 'alice@example.com')
         * ON CONFLICT (id) DO UPDATE
         * SET name = EXCLUDED.name,
         *     email = EXCLUDED.email;
         */
        StringBuilder builder = new StringBuilder(255);
        builder.append("INSERT INTO ").append(table).append(" (");

        Map<String, FieldDefine> fieldsMap = defines.getFieldsMap();
        String[] columns = new String[fieldsMap.size()];
        {
            int i = 0;
            for (Map.Entry<String, FieldDefine> entry : fieldsMap.entrySet()) {
                String col = entry.getKey();
                columns[i++] = col;
                builder.append("\"").append(col).append("\",");
            }
        }
        builder.setCharAt(builder.length() - 1, ')');
        builder.append(" VALUES ");

        String[] pks = defines.getUniqueKeys();
        if (pks == null) {
            pks = new String[0];
        }
        LinkedHashMap<String, Map<String, Object>> ids;
        if (pks.length > 0) {
            ids = new LinkedHashMap<>(list.size());
            for (Map<String, Object> bean : list) {
                String idStr = "";
                if (pks.length == 1) {
                    Object id = bean.get(pks[0]);
                    if (id == null) {
                        // id 为空，当做不重复处理
                        ids.put(String.valueOf(System.identityHashCode(bean)), bean);
                        continue;
                    }
                    idStr = String.valueOf(id);
                } else {
                    StringBuilder idBd = new StringBuilder();
                    for (String k : pks) {
                        idBd.append(bean.get(k)).append('`');
                    }
                    idStr = idBd.toString();
                }
                if (!"null".equals(idStr) && !"".equals(idStr)) {
                    ids.put(idStr, bean);
                }
            }
            list = ids.values();
        }
        for (Map<String, Object> bean : list) {
            builder.append('(');
            for (String f : columns) {
                Object val = bean.get(f);
                if (val != null) {
                    FieldDefine define = fieldsMap.get(f);
                    if (define != null) {
                        FieldType fieldType = define.getType();
                        if (fieldType == FieldType.DATETIME && val instanceof Long) {
                            val = getDateTimeStr(val);
                        } else if (fieldType == FieldType.STRING) {
                            // postgresql 的单引号处理方式：特殊语法，双单引号代替单个单引号
                            val = val.toString().replace("'", "''");
                        }
                    }
                    builder.append('\'').append(val).append("',");
                } else {
                    builder.append("DEFAULT,");
                }
            }
            builder.setCharAt(builder.length() - 1, ')');
            builder.append(',');
        }
        builder.setCharAt(builder.length() - 1, ' ');
        if (pks != null && pks.length > 0) {
            builder.append("ON CONFLICT(");
            for (String f : pks) {
                builder.append('"').append(f).append("\",");
            }
            builder.setCharAt(builder.length() - 1, ')');
            builder.append(" do update set ");
            for (String f : columns) {
                builder.append('"').append(f).append("\"=EXCLUDED.\"").append(f).append("\",");
            }
            builder.setCharAt(builder.length() - 1, ' ');
        }
        String insertSQL = builder.toString();
        return insertSQL;
    }

    static String getCreateTableSQL(String tableName, FieldDefine[] fs) {
        FieldDefineVo[] fields = new FieldDefineVo[fs.length];
        for (int i = 0; i < fs.length; i++) {
            fields[i] = new FieldDefineVo(fs[i]);
        }

        Set<String> ids = processFieldTypes(fields);
        StringBuilder builder = new StringBuilder(128);
        builder.append("create table IF NOT EXISTS ").append(tableName).append(" (");
        for (int i = 0; i < fields.length; i++) {
            FieldDefineVo def = fields[i];
            String type = def.getType();
            String name = def.getName();
            if (def.isUnique()) {
                if ("integer".equals(type)) {
                    type = "Serial";
                } else if ("bigint".equals(type)) {
                    type = "bigSerial";
                }
            }
            type = getStringType(fs[i], type);
            builder.append("\"").append(name).append("\" ").append(type);
            if (def.isUnique()) {
                builder.append(" NOT NULL ");
            } else if (type.startsWith("timestamp")) {
                builder.append(" DEFAULT now() ");
            }
            builder.append(',');
        }
        if (ids.size() > 0) {
            String table = DbTableNameUtils.getCleanTableName(tableName);
            int x = table.lastIndexOf('/');
            if (x > 0) {
                table = table.substring(x + 1);
            }
            builder.append("CONSTRAINT \"pk_").append(table).append("_").append(IdUtil.getSnowflake().nextIdStr()).append("\" PRIMARY KEY (");
            for (String pk : ids) {
                builder.append("\"").append(pk).append("\",");
            }
            builder.setCharAt(builder.length() - 1, ')');
            builder.append(' ');
        }
        builder.setCharAt(builder.length() - 1, ')');
        String sql = builder.toString();
        return sql;
    }

    static Set<String> processFieldTypes(FieldDefineVo[] fields) {
        LinkedHashSet<String> pks = new LinkedHashSet<>();
        for (FieldDefineVo def : fields) {
            String col = def.getName();
            if (def.isUnique()) {
                pks.add(col);
            }
        }

        for (FieldDefineVo def : fields) {
            String type = def.getType();
            String name = def.getName();
            if (!pgTypeMap.contains(type)) {
                switch (def.getType().toLowerCase()) {
                    case "int":
                        type = "integer";
                        break;
                    case "long":
                        type = "bigint";
                        break;
                    case "double":
                        type = "float8";
                        if (pks.contains(name)) {
                            type = "bigint";
                        }
                        break;
                    case "number":
                        if (pks.contains(name)) {
                            type = "bigint";
                        } else {
                            type = "float";
                        }
                        break;
                    case "string":
                        type = "text";
                        String nameC = name.toLowerCase();
                        if (nameC.contains("name")) {
                            type = "varchar(128)";
                        } else if (nameC.contains("id")) {
                            type = "varchar(64)";
                        } else if (nameC.contains("json")) {
                            type = "jsonb";
                        }
                        break;
                    case "boolean":
                        type = "boolean";
                        break;
                    case "datetime":
                        type = "timestamptz(3)";
                        break;
                }
                def.setType(type);
            } else if (type.equals("float")) {
                if (pks.contains(name)) {
                    def.setType("bigint");
                } else {
                    def.setType("float4");
                }
            }
        }
        return pks;
    }


}
