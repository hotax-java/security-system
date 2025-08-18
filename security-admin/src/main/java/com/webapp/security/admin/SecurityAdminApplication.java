package com.webapp.security.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端应用启动类
 */
@SpringBootApplication(scanBasePackages = { "com.webapp.security.admin", "com.webapp.security.core" })
@MapperScan("com.webapp.security.core.mapper")
public class SecurityAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecurityAdminApplication.class, args);
    }
}
