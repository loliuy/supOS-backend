package com.supos.adpter.pg.config;

import com.supos.adpter.pg.PostgresqlEventHandler;
import com.supos.adpter.pg.TimeScaleDbEventHandler;
import com.supos.common.adpater.DataSourceProperties;
import com.supos.common.adpater.TimeSequenceDataStorageAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Configuration("pgConfig")
public class PostgresqlConfig {

    @Bean
    @Order(1)
    public PostgresqlEventHandler pgHandler(@Value("${pg.jdbcUrl:}") String jdbcUrl,
                                            @Value("${spring.datasource.url}") String suposJdbcUrl,
                                            @Value("${pg.user:postgres}") String user,
                                            @Value("${pg.psw:postgres}") String password,
                                            @Value("${pg.schema:public}") String schema) {
        JdbcTemplate template = jdbcTemplate(jdbcUrl, suposJdbcUrl, user, password, schema);
        return new PostgresqlEventHandler(template);
    }

    @Bean
    @ConditionalOnMissingBean(TimeSequenceDataStorageAdapter.class)
    @Order
    public TimeScaleDbEventHandler timeScaleDbEventHandler(
            ApplicationContext beanFactory,
            @Autowired PostgresqlEventHandler pgHandler) {
        try {
            Map<String, TimeSequenceDataStorageAdapter> other = beanFactory.getBeansOfType(TimeSequenceDataStorageAdapter.class);
            if (!other.isEmpty()) {
                return null;
            }
        } catch (Exception ex) {
            log.warn("NO TimeSequenceDataStorageAdapter, use Default TimeScaleDB {}", ex.getMessage());
        }
        return new TimeScaleDbEventHandler(pgHandler.getJdbcTemplate());
    }


    static class PgDataSource extends HikariDataSource implements DataSourceProperties {
        PgDataSource(HikariConfig config) {
            super(config);
        }

        @Override
        public String getUrl() {
            return super.getJdbcUrl();
        }
    }
    private JdbcTemplate jdbcTemplate(String jdbcUrl,
                                      String suposJdbcUrl,
                                      String user,
                                      String password,
                                      String schema// 写关系表数据的 schema
    ) {
        if (!StringUtils.hasText(jdbcUrl)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                jdbcUrl = "jdbc:postgresql://100.100.100.20:31014/postgres";
            } else if (suposJdbcUrl.startsWith("jdbc:postgresql:")) {
                jdbcUrl = suposJdbcUrl;
            } else {
                jdbcUrl = "jdbc:postgresql://postgresql:5432/postgres";
            }
        }
        HikariConfig config = new HikariConfig();
        config.setUsername(user);
        config.setPassword(password);
        config.setSchema(schema);
        // connection pool configurations
        config.setMinimumIdle(10); // minimum number of idle connection
        config.setMaximumPoolSize(20); // maximum number of connection in the pool
        config.setConnectionTimeout(30000); // maximum wait milliseconds for get connection from pool
        config.setMaxLifetime(0); // maximum life time for each connection
        config.setIdleTimeout(0); // max idle time for recycle idle connection
        config.setConnectionTestQuery("SELECT 1"); // validation query
        config.setJdbcUrl(jdbcUrl);
        HikariDataSource dataSource = new PgDataSource(config); // create datasource
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.execute("create schema if not exists " + schema);
        return template;
    }


}
