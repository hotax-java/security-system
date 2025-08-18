package com.webapp.security.sso.third.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 支付宝用户服务
 * 处理支付宝OAuth2认证和用户信息获取
 */
@Service
public class AlipayUserService {
    private static final Logger logger = LoggerFactory.getLogger(AlipayUserService.class);

    private final AlipayOAuth2Config alipayConfig;
    private final RestTemplate restTemplate;
    private AlipayClient alipayClient;

    public AlipayUserService(AlipayOAuth2Config alipayConfig, RestTemplate restTemplate) {
        this.alipayConfig = alipayConfig;
        this.restTemplate = restTemplate;

        // 初始化AlipayClient
        this.alipayClient = new DefaultAlipayClient(
                alipayConfig.getGatewayUrl(),
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                "json",
                "UTF-8",
                alipayConfig.getPublicKey(),
                "RSA2");
    }

    /**
     * 获取支付宝授权URL
     */
    public String getAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(alipayConfig.getAuthorizeUrl())
                .queryParam("app_id", alipayConfig.getAppId())
                .queryParam("redirect_uri", alipayConfig.getRedirectUri())
                .queryParam("scope", alipayConfig.getScope())
                .queryParam("state", state)
                .build().toUriString();
    }

    /**
     * 获取访问令牌
     */
    public AlipayAccessToken getAccessToken(String authCode) {
        try {
            AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
            request.setCode(authCode);
            request.setGrantType("authorization_code");

            AlipaySystemOauthTokenResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                AlipayAccessToken token = new AlipayAccessToken();
                token.setAccessToken(response.getAccessToken());
                token.setRefreshToken(response.getRefreshToken());
                token.setExpiresIn(response.getExpiresIn());
                token.setReExpiresIn(response.getReExpiresIn());
                
                // 优先使用openId，如果没有则使用userId
                String userId = response.getOpenId();
                if (userId == null || userId.isEmpty()) {
                    userId = response.getUserId();
                }
                token.setUserId(userId);
                
                logger.info("支付宝授权成功，userId: {}, openId: {}, 最终使用: {}", 
                    response.getUserId(), response.getOpenId(), userId);
                return token;
            } else {
                logger.error("获取支付宝访问令牌失败: {}, {}", response.getCode(), response.getMsg());
            }
        } catch (AlipayApiException e) {
            logger.error("调用支付宝访问令牌接口异常", e);
        }

        return null;
    }

    /**
     * 获取支付宝用户信息（通过访问令牌）
     */
    public AlipayUserInfo getUserInfoByToken(String accessToken) {
        if (accessToken == null) {
            return null;
        }
        try {
            AlipayUserInfoShareRequest request = new AlipayUserInfoShareRequest();
            AlipayUserInfoShareResponse response = alipayClient.execute(request, accessToken);

            if (response.isSuccess()) {
                AlipayUserInfo userInfo = new AlipayUserInfo();
                
                // 优先使用openId，如果没有则使用userId
                String userId = response.getOpenId();
                if (userId == null || userId.isEmpty()) {
                    userId = response.getUserId();
                }
                userInfo.setUserId(userId);
                
                userInfo.setNickName(response.getNickName());
                userInfo.setAvatar(response.getAvatar());
                userInfo.setGender(response.getGender());
                userInfo.setProvince(response.getProvince());
                userInfo.setCity(response.getCity());
                
                logger.info("获取支付宝用户信息成功，userId: {}, openId: {}, 最终使用: {}", 
                    response.getUserId(), response.getOpenId(), userId);
                return userInfo;
            } else {
                logger.error("获取支付宝用户信息失败: {}, {}", response.getCode(), response.getMsg());
            }
        } catch (AlipayApiException e) {
            logger.error("调用支付宝用户信息接口异常", e);
        }

        return null;
    }

    /**
     * 一次性完成从授权码到用户信息的获取
     */
    public AlipayUserInfo getUserInfo(String code) {
        AlipayAccessToken accessToken = getAccessToken(code);
        if (accessToken != null) {
            return getUserInfoByToken(accessToken.getAccessToken());
        }
        return null;
    }

    /**
     * 支付宝访问令牌响应
     */
    @Data
    public static class AlipayAccessToken {
        private String accessToken;
        private String refreshToken;
        private String userId;
        private String expiresIn;
        private String reExpiresIn;
    }

    /**
     * 支付宝用户信息响应
     */
    @Data
    public static class AlipayUserInfo {
        private String userId;
        private String nickName;
        private String avatar;
        private String gender;
        private String province;
        private String city;
    }
}