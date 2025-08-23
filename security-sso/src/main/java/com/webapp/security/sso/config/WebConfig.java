package com.webapp.security.sso.config;

import com.webapp.security.sso.interceptor.ClientIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
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

        // 注意：这是开发环境的宽松配置
        // 生产环境应该限制为特定的来源、方法和头部
        config.addAllowedOriginPattern("*"); // 开发环境允许所有源
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true); // 必须设置为true以允许认证信息（如cookie）跨域传输
        config.setMaxAge(3600L);

        // 由于前后端整合在同一域下运行，此CORS配置主要用于开发环境
        // 一旦前端构建到static目录，同域请求将不再需要CORS
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
