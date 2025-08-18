package com.webapp.security.core.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis-Plus配置类
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.webapp.security.core.mapper")
public class MybatisPlusConfig {

    /**
     * 自动填充处理器
     */
    @Bean
    public MyBatisPlusMetaObjectHandler myBatisPlusMetaObjectHandler() {
        return new MyBatisPlusMetaObjectHandler();
    }
}