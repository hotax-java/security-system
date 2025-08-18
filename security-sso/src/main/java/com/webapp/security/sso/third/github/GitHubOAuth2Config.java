package com.webapp.security.sso.third.github;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub OAuth2配置类
 */
@Configuration
@ConfigurationProperties(prefix = "oauth2.github")
@Data
public class GitHubOAuth2Config {

    /**
     * GitHub应用ID
     */
    private String appId = "Ov23liqBqh7ITsj5hiLu";

    /**
     * GitHub应用密钥
     */
    private String appSecret = "9fda03ddd8b9f25be2d58df0a95381bc4ab8d196";

    /**
     * 回调地址
     */
    private String redirectUri = "http://localhost:9000/auth/github/callback";

    /**
     * 授权URL
     */
    private String authorizeUrl = "https://github.com/login/oauth/authorize";

    /**
     * 获取令牌URL
     */
    private String tokenUrl = "https://github.com/login/oauth/access_token";

    /**
     * 获取用户信息URL
     */
    private String userInfoUrl = "https://api.github.com/user";

    /**
     * 获取用户邮箱URL
     */
    private String userEmailsUrl = "https://api.github.com/user/emails";

    /**
     * 授权范围
     */
    private String scope = "user:email";

    /**
     * 前端回调URL
     */
    private String frontendCallbackUrl;
}