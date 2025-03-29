package com.supos.uns.config;

import com.supos.common.Constants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * @author xinwangji@supos.com
 * @date 2021/5/21 16:17
 * @description MVC 配置
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {


    /**
     * 文件路径映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations(
                "file:" + Constants.ROOT_PATH + "/");
    }

}
