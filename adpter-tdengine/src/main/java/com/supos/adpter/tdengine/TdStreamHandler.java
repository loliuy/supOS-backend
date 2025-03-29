package com.supos.adpter.tdengine;

import com.supos.common.adpater.StreamHandler;
import com.supos.common.dto.StreamInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Types;
import java.util.*;

@Slf4j
class TdStreamHandler implements StreamHandler {
    final JdbcTemplate jdbcTemplate;

    TdStreamHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createStream(Map<String, String> namedSQL) {
        log.info("创建流计算: {}", namedSQL);
        String[] sqls = new String[namedSQL.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : namedSQL.entrySet()) {
            String name = entry.getKey();
            String sql = entry.getValue();
            sqls[i++] = "DROP STREAM if exists `" + name + "`";
            sqls[i++] = sql;
        }
        jdbcTemplate.batchUpdate(sqls);
    }

    @Override
    public List<StreamInfo> listByNames(Collection<String> names) {
        if (names == null) {
            names = Collections.emptyList();
        }
        final int SIZE = names.size();
        StringBuilder sql = new StringBuilder(64 + SIZE * 32);
        sql.append("select stream_name, status, target_table, sql from information_schema.`ins_streams` ");
        if (SIZE > 0) {
            sql.append(" where stream_name ");
            if (SIZE == 1) {
                sql.append("=?");
            } else {
                sql.append(" in (");
                for (int i = 0; i < SIZE; i++) {
                    sql.append("?,");
                }
                sql.setCharAt(sql.length() - 1, ')');
            }
        }
        int[] types = new int[SIZE];
        Arrays.fill(types, Types.VARCHAR);
        return jdbcTemplate.query(sql.toString(), names.toArray(), types, rs -> {
            List<StreamInfo> list = new ArrayList<>(8 + SIZE);
            while (rs.next()) {
                String name = rs.getString(1);
                String statusStr = rs.getString(2);
                String table = rs.getString(3);
                String createSQL = rs.getString(4);
                final int state = "ready".equalsIgnoreCase(statusStr) ? StreamInfo.STATUS_RUNNING :
                        ("paused".equalsIgnoreCase(statusStr) ? StreamInfo.STATUS_PAUSED : 0);
                StreamInfo streamInfo = new StreamInfo(name, table, state);
                streamInfo.setSql(createSQL);
                list.add(streamInfo);
            }
            return list;
        });
    }

    @Override
    public void deleteStream(String name) {
        log.info("删除流计算: {}", name);
        jdbcTemplate.update("DROP STREAM if exists `" + name + "`");
    }

    @Override
    public void stopStream(String name) {
        log.info("停止流计算: {}", name);
        jdbcTemplate.update("PAUSE STREAM `" + name + "`");
    }

    @Override
    public void resumeStream(String name) {
        log.info("恢复流计算: {}", name);
        jdbcTemplate.update("RESUME STREAM if exists `" + name + "`");
    }


}
