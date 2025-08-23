package com.webapp.security.sso.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 测试环境的安全配置
 * 用于PKCE授权码流程测试
 */
@Configuration
@EnableWebSecurity
@Order(99) // 确保这个过滤器链在默认链之前执行
public class TestSecurityConfig {

    /**
     * 测试用户配置
     * 创建一个内存中的用户，用于测试登录
     */
    @Bean
    public UserDetailsService testUserDetailsService() {
        // 警告：这仅用于测试目的，使用了不安全的密码编码器
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin123")
                .roles("USER", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
}