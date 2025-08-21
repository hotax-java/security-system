package com.webapp.security.admin.auth.oauth2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2客户端配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "webapp.oauth2")
public class OAuth2ClientProperties {

    /**
     * 是否启用PKCE支持，默认为false
     */
    private boolean enablePkce = false;

    public boolean isEnablePkce() {
        return enablePkce;
    }

    public void setEnablePkce(boolean enablePkce) {
        this.enablePkce = enablePkce;
    }
}