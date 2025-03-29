package com.supos.adpter.minio.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/8 15:18
 */
@Configuration
public class MinioConfig {

    /**默认桶名*/
    public static final String DEFAULT_BUCKET = "uns-attachment";

    @Value("${minio.endpoint:http://minio:9000}")
    private String endpoint;

    @Value("${minio.accessKey:admin}")
    private String accessKey;

    @Value("${minio.secreKey:adminpassword}")
    private String secreKey;


    @Bean
    public MinioClient minioClient() throws MalformedURLException {
        if (!StringUtils.hasText(endpoint)) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                endpoint = "http://office.unibutton.com:11490";
            } else {
                endpoint = "http://minio:9000";
            }
        }
        return MinioClient.builder().endpoint(URI.create(endpoint).toURL()).credentials(accessKey, secreKey).build();
    }
}
