package com.webapp.security.sso.oauth2.custom.config;

import com.webapp.security.sso.oauth2.custom.filter.CustomJwtAuthenticationFilter;
import com.webapp.security.sso.oauth2.custom.service.CustomJwtUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 自定义JWT安全配置
 * 可以通过配置开关启用或禁用
 */
@Configuration
//@EnableWebSecurity
@ConditionalOnProperty(name = "custom.jwt.enabled", havingValue = "true", matchIfMissing = false)
public class CustomJwtSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(CustomJwtSecurityConfig.class);

    private final CustomJwtAuthenticationFilter customJwtAuthenticationFilter;
    private final CustomJwtUserDetailsService customJwtUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomJwtSecurityConfig(CustomJwtAuthenticationFilter customJwtAuthenticationFilter,
                                   CustomJwtUserDetailsService customJwtUserDetailsService, PasswordEncoder passwordEncoder) {
        this.customJwtAuthenticationFilter = customJwtAuthenticationFilter;
        this.customJwtUserDetailsService = customJwtUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        log.info("Custom JWT security configuration initialized");
    }

    /**
     * 自定义JWT安全过滤器链
     * 优先级设置为3，低于OAuth2授权服务器(1)和默认安全配置(2)
     */
    @Bean
    @Order(3)
    public SecurityFilterChain customJwtSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring custom JWT security filter chain");

        http
                .antMatcher("/api/custom/**") // 只对自定义JWT接口生效
                .authorizeHttpRequests((authorize) -> authorize
                        .antMatchers("/api/custom/jwt/login", "/api/custom/jwt/refresh",
                                "/api/custom/jwt/validate", "/api/custom/jwt/userinfo")
                        .permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 添加自定义JWT过滤器
                .addFilterBefore(customJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 配置认证提供者
                .authenticationProvider(customJwtAuthenticationProvider());

        return http.build();
    }

    /**
     * 自定义JWT认证提供者
     */
    @Bean
    public DaoAuthenticationProvider customJwtAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customJwtUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}