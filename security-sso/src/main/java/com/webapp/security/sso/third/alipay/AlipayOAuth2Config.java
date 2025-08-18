package com.webapp.security.sso.third.alipay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 支付宝OAuth2配置
 */
@Configuration
@ConfigurationProperties(prefix = "oauth2.alipay")
@Data
public class AlipayOAuth2Config {
    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String publicKey;

    /**
     * 支付宝网关URL
     */
    private String gatewayUrl;

    /**
     * 回调地址
     */
    private String redirectUri;

    /**
     * 授权范围
     */
    private String scope = "auth_user";

    /**
     * 支付宝授权URL
     */
    private String authorizeUrl = "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm";

    /**
     * 获取访问令牌URL
     */
    private String tokenUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 获取用户信息URL
     */
    private String userInfoUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 前端回调URL
     */
    private String frontendCallbackUrl;

}