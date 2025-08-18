package com.webapp.security.sso.third.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信OAuth2配置类
 */
@Configuration
@ConfigurationProperties(prefix = "oauth2.wechat")
@Data
public class WechatOAuth2Config {

    /**
     * 微信开放平台应用ID
     */
    private String appId;

    /**
     * 微信开放平台应用密钥
     */
    private String appSecret;

    /**
     * 授权回调地址
     */
    private String redirectUri;

    /**
     * 微信授权URL
     */
    private String authorizeUrl = "https://open.weixin.qq.com/connect/qrconnect";

    /**
     * 获取访问令牌URL
     */
    private String accessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

    /**
     * 获取用户信息URL
     */
    private String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo";

    private String frontendCallbackUrl;
}