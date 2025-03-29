package com.supos.conf;

import cn.hutool.extra.spring.EnableSpringUtil;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.mybatis.spring.boot.test.autoconfigure.AutoConfigureMybatis;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.supos")
@AutoConfigureMybatis
@ImportAutoConfiguration(classes = MybatisPlusAutoConfiguration.class)
@EnableSpringUtil
public class TestCaseInitConfiguration {
}
