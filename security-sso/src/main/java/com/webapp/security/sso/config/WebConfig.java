package com.webapp.security.sso.config;

import com.webapp.security.sso.oauth2.interceptor.ClientIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ClientIdInterceptor clientIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientIdInterceptor)
                .addPathPatterns("/login", "/logout", "/refresh", "/oauth2/**") // 包含认证相关接口
                .excludePathPatterns("/oauth2/health"); // 排除健康检查接口
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOriginPattern("*"); // 允许所有源
        config.addAllowedHeader("*"); // 允许所有头部
        config.addAllowedMethod("*"); // 允许所有方法
        config.setAllowCredentials(true); // 允许携带认证信息
        config.setMaxAge(3600L); // 预检请求缓存时间

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
