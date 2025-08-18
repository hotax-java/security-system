package com.webapp.security.sso;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证授权服务启动类
 * 集成鉴权(Authentication)和授权(Authorization)功能
 * 
 * @author webapp-auth-system
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.webapp.security")
@MapperScan({"com.webapp.security.sso.*.mapper", "com.webapp.security.core.mapper"})
public class SecuritySSOApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecuritySSOApplication.class, args);
    }
}

