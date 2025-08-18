package com.webapp.security.sso.third.wechat;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

/**
 * 微信用户服务
 * 处理微信OAuth2认证和用户信息获取
 */
@Service
public class WechatUserService {

    private static final Logger logger = LoggerFactory.getLogger(WechatUserService.class);

    @Autowired
    private WechatOAuth2Config wechatConfig;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取微信授权URL
     */
    public String getAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(wechatConfig.getAuthorizeUrl())
                .queryParam("appid", wechatConfig.getAppId())
                .queryParam("redirect_uri", wechatConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_base")
                .queryParam("state", state)
                .build().toUriString() + "#wechat_redirect";
    }

    /**
     * 获取访问令牌
     */
    public WechatAccessToken getAccessToken(String code) {
        String url = UriComponentsBuilder.fromHttpUrl(wechatConfig.getAccessTokenUrl())
                .queryParam("appid", wechatConfig.getAppId())
                .queryParam("secret", wechatConfig.getAppSecret())
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .build().toUriString();

        try {
            ResponseEntity<WechatAccessToken> response = restTemplate.getForEntity(url, WechatAccessToken.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                WechatAccessToken token = response.getBody();
                if (token != null && token.getErrcode() == 0) {
                    return token;
                } else {
                    logger.error("获取微信访问令牌失败: {}", token != null ? token.getErrmsg() : "未知错误");
                }
            }
        } catch (RestClientException e) {
            logger.error("调用微信访问令牌接口异常", e);
        }

        return null;
    }

    /**
     * 获取用户信息
     */
    public WechatUserInfo getUserInfo(WechatAccessToken accessToken) {
        if (accessToken == null) {
            return null;
        }

        String url = UriComponentsBuilder.fromHttpUrl(wechatConfig.getUserInfoUrl())
                .queryParam("access_token", accessToken.getAccessToken())
                .queryParam("openid", accessToken.getOpenid())
                .queryParam("lang", "zh_CN")
                .build().toUriString();

        try {
            ResponseEntity<WechatUserInfo> response = restTemplate.getForEntity(url, WechatUserInfo.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                WechatUserInfo userInfo = response.getBody();
                if (userInfo != null && userInfo.getErrcode() == 0) {
                    return userInfo;
                } else {
                    logger.error("获取微信用户信息失败: {}", userInfo != null ? userInfo.getErrmsg() : "未知错误");
                }
            }
        } catch (RestClientException e) {
            logger.error("调用微信用户信息接口异常", e);
        }

        return null;
    }

    /**
     * 获取用户信息（一次性完成code到用户信息的转换）
     */
    public WechatUserInfo getUserInfo(String code) {
        WechatAccessToken accessToken = getAccessToken(code);
        return getUserInfo(accessToken);
    }

    /**
     * 微信访问令牌响应
     */
    @Data
    public static class WechatAccessToken {
        private String accessToken;
        private String refreshToken;
        private int expiresIn;
        private String openid;
        private String scope;
        private String unionid;
        private int errcode;
        private String errmsg;

        // Jackson映射字段名
        public void setAccess_token(String accessToken) {
            this.accessToken = accessToken;
        }

        public void setRefresh_token(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public void setExpires_in(int expiresIn) {
            this.expiresIn = expiresIn;
        }

        /**
         * 计算过期时间
         */
        public LocalDateTime getExpiresAt() {
            return LocalDateTime.now().plusSeconds(expiresIn);
        }
    }

    /**
     * 微信用户信息响应
     */
    @Data
    public static class WechatUserInfo {
        private String openid;
        private String nickname;
        private int sex;
        private String province;
        private String city;
        private String country;
        private String headimgurl;
        private String[] privilege;
        private String unionid;
        private int errcode;
        private String errmsg;
    }
}