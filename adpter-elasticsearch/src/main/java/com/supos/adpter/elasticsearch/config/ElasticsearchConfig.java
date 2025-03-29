package com.supos.adpter.elasticsearch.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author sunlifang
 * @version 1.0
 * @description: es连接配置
 * @date 2024/12/19 13:52
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${ELASTICSEARCH_HOST:}")
    private String host;
    @Value("${ELASTICSEARCH_PORT:9200}")
    private int port;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        if (!StringUtils.hasText(host)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                host = "office.unibutton.com";
                port = 11492;
            } else {
                host = "elasticsearch";
                port = 9200;
            }
        }
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host, port, "http")));
        return client;
    }
}
