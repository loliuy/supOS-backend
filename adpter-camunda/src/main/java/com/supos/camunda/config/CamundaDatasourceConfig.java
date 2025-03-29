package com.supos.camunda.config;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Configuration
public class CamundaDatasourceConfig {

    @Bean("camundaProperties")
    @ConfigurationProperties("camunda.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }
//
//
//    @Bean(name = "camundaDataSource")
//    public DataSource camundaDataSource(@Qualifier("camundaProperties")DataSourceProperties properties) {
//        return properties.initializeDataSourceBuilder().build();
//    }

//    @Bean
//    public PlatformTransactionManager camundaTransactionManager(
//            @Qualifier("camundaDataSource") DataSource camundaDataSource) {
//        return new DataSourceTransactionManager(camundaDataSource);
//    }

    @Bean
    public ProcessEngineConfigurationImpl processEngineConfiguration(@Qualifier("camundaProperties")DataSourceProperties properties) {
        DataSource camundaDataSource = properties.initializeDataSourceBuilder().build();
        PlatformTransactionManager camundaTransactionManager = new DataSourceTransactionManager(camundaDataSource);
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(camundaDataSource);
        config.setTransactionManager(camundaTransactionManager);
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setHistory(ProcessEngineConfiguration.HISTORY_FULL);
        return config;
    }
}
