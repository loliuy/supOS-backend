package com.supos.adpter.pg;

import com.supos.common.adpater.DataSourceProperties;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

public class PostgresqlBase {
    @Getter
    final JdbcTemplate jdbcTemplate;
    final String currentSchema;
    final TransactionTemplate transactionTemplate;

    PostgresqlBase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        DataSource dataSource = jdbcTemplate.getDataSource();
        DataSourceProperties dataSourceProperties = (DataSourceProperties) dataSource;
        currentSchema = dataSourceProperties.getSchema();
        JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public DataSourceProperties getDataSourceProperties() {
        return (DataSourceProperties) jdbcTemplate.getDataSource();
    }

    protected void doTx(java.lang.Runnable dbTask) {
        transactionTemplate.executeWithoutResult(transactionStatus -> dbTask.run());
    }
}
