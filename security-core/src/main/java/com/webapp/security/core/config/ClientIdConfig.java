package com.webapp.security.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 客户端ID配置
 * 用于管理OAuth2客户端ID
 */
@Configuration
@ConfigurationProperties(prefix = "oauth2.client")
@Data
public class ClientIdConfig {

    /**
     * API客户端ID
     */
    private String apiClientId = "openapi";

    /**
     * Web应用客户端ID
     */
    private String webappClientId = "webapp-client";
}