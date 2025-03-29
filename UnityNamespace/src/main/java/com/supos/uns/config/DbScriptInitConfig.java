package com.supos.uns.config;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DatabasePopulator.class)
@Conditional(DbScriptInitConfig.OnBatchDatasourceInitializationCondition.class)
@EnableConfigurationProperties(BatchProperties.class)
public class DbScriptInitConfig implements BeanPostProcessor {

    @Bean
    @ConditionalOnMissingBean({BatchDataSourceScriptDatabaseInitializer.class})
    BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(DataSource dataSource,
                                                                        @BatchDataSource ObjectProvider<DataSource> batchDataSource, BatchProperties properties) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource ds = (HikariDataSource) dataSource;
            tryCreateSchema(ds, ds.getJdbcUrl());
        }
        return new BatchDataSourceScriptDatabaseInitializer(batchDataSource.getIfAvailable(() -> dataSource),
                properties.getJdbc());
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DefaultDataSourceCreator) {
            return new InitializerDefaultDataSourceCreator((DefaultDataSourceCreator) bean);
        }
        return bean;
    }

    static class InitializerDefaultDataSourceCreator extends DefaultDataSourceCreator {
        final DefaultDataSourceCreator tar;

        InitializerDefaultDataSourceCreator(DefaultDataSourceCreator tar) {
            this.tar = tar;
        }

        public DataSource createDataSource(DataSourceProperty dataSourceProperty) {
            DataSource ds = tar.createDataSource(dataSourceProperty);
            tryCreateSchema(ds, dataSourceProperty.getUrl());
            return ds;
        }

    }

    private static void tryCreateSchema(DataSource ds, String url) {
        if (url.indexOf('?') > 0 && url.startsWith("jdbc:")) {
            int protoIndex = url.indexOf("://");
            String type = url.substring(5, protoIndex);
            if ("postgresql".equals(type)) {
                String uri = url.substring(protoIndex);
                UriComponents components = UriComponentsBuilder.fromUriString("http" + uri).build();
                MultiValueMap<String, String> queryMap = components.getQueryParams();
                String currentSchema = queryMap.getFirst("currentSchema");
                if (StringUtils.hasText(currentSchema)) {
                    try (Connection conn = ds.getConnection()) {
                        try (Statement statement = conn.createStatement()) {
                            String ddl = "create schema if not exists " + currentSchema;
                            log.info("postgresql initSchema: {}, jdbcUrl = {}", currentSchema, url);
                            statement.execute(ddl);
                        }
                    } catch (Exception ex) {
                        log.error("Fail to init schema: " + currentSchema, ex);
                    }
                }
            }
        }
    }

    static class OnBatchDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

        OnBatchDatasourceInitializationCondition() {
            super("Batch", "spring.batch.jdbc.initialize-schema", "spring.batch.initialize-schema");
        }

    }
}
