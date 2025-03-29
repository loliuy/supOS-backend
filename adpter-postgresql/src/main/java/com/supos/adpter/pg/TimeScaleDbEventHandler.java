package com.supos.adpter.pg;

import cn.hutool.core.lang.Pair;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.adpater.StreamHandler;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.supos.common.annotation.Description;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.SaveDataDto;
import com.supos.common.dto.TopologyLog;
import com.supos.common.enums.FieldType;
import com.supos.common.event.BatchCreateTableEvent;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.event.SaveDataEvent;
import com.supos.common.utils.DbTableNameUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.common.utils.PostgresqlTypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

import static com.supos.adpter.pg.PostgresqlEventHandler.getCreateTableSQL;
import static com.supos.adpter.pg.PostgresqlEventHandler.getInsertSQL;

@Slf4j
public class TimeScaleDbEventHandler extends PostgresqlBase implements TimeSequenceDataStorageAdapter {

    public TimeScaleDbEventHandler(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public String name() {
        return "TimeScaleDBAdapter";
    }

    @Override
    public SrcJdbcType getJdbcType() {
        return SrcJdbcType.TimeScaleDB;
    }

    @Override
    public StreamHandler getStreamHandler() {
        return new TimeScaleStreamHandler();
    }

    @Override
    public String execSQL(String sql) {
        if (sql.toLowerCase().startsWith("select")) {
            List<Map> rs = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Map.class));
            return JsonUtil.toJson(rs);
        } else {
            jdbcTemplate.execute(sql);
            return "ok";
        }
    }

    @EventListener(classes = BatchCreateTableEvent.class)
    @Order(9)
    @Description("uns.create.task.name.tmsc")
    public void onBatchCreateTableEvent(BatchCreateTableEvent event) {
        CreateTopicDto[] topics;
        if (event.getSource() != this && ArrayUtils.isNotEmpty(topics = event.topics.get(SrcJdbcType.TimeScaleDB))) {
            super.doTx(() -> batchCreateTables(topics));
        }
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(9)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        if (event.jdbcType != SrcJdbcType.TimeScaleDB) {
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
            log.debug("TimeScaleDB 删除：{}", sqls);
            jdbcTemplate.batchUpdate(sqls.toArray(new String[0]));
        }
    }

    @EventListener(classes = SaveDataEvent.class)
    @Order(9)
    void onSaveData(SaveDataEvent event) {
        if (SrcJdbcType.TimeScaleDB == event.jdbcType && ArrayUtils.isNotEmpty(event.topicData) && event.getSource() != this) {
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
                log.debug("PgTimeScale Write: \n{}", sqlList);
                String[] sqlArray = sqlList.toArray(new String[0]);
                try {
                    jdbcTemplate.batchUpdate(sqlArray);
                } catch (Exception ex) {
                    Set<String> topics = sqlPairList.stream().map(s -> s.getKey().getTopic()).collect(Collectors.toSet());
                    TopologyLog.log(topics, TopologyLog.Node.DATA_PERSISTENCE, TopologyLog.EventCode.ERROR, I18nUtils.getMessage("uns.topology.db.pg"));
                    CreateTopicDto[] dtos = sqlPairList.stream().map(s -> s.getKey().getCreateTopicDto())
                            .toArray(CreateTopicDto[]::new);
                    log.error("PgTimeScale 写入失败: {}, {}", ex.getMessage(), sqlList);
                    try {
                        batchCreateTables(dtos);
                        jdbcTemplate.batchUpdate(sqlArray);
                        log.debug("retry success!");
                    } catch (Exception rex) {
                        log.error("PgTimeScale 写入 re失败:" + topics, rex);
                    }
                }
            }
        }
    }

    void batchCreateTables(CreateTopicDto[] topics) {
        // 批量执行 TimeScaleDB 建表
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
                }
            }
            createTableSQLs.add(createTableSQL);
            if (oldFieldTypes.isEmpty()) {
                createTableSQLs.add("SELECT create_hypertable('" + dbName + ".\"" + tableName + "\"', '" + Constants.SYS_FIELD_CREATE_TIME + "',chunk_time_interval => INTERVAL '1 day')");
            }
        }
        log.debug("PgTimeScale CreateTable: {} {}", createTableSQLs.size(), createTableSQLs);
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


}
