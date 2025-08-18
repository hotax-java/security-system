package com.webapp.security.sso.api.service;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.security.SecureRandom;

/**
 * 自定义不透明令牌生成器
 * 生成更短的不透明令牌
 */
public class ShortOpaqueTokenGenerator implements OAuth2TokenGenerator<OAuth2Token> {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public OAuth2Token generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null ||
                !context.getTokenType().getValue().equals(OAuth2TokenType.ACCESS_TOKEN.getValue())) {
            return null;
        }

        // 从客户端配置中获取令牌有效期
        RegisteredClient registeredClient = context.getRegisteredClient();
        Duration accessTokenTimeToLive = registeredClient.getTokenSettings().getAccessTokenTimeToLive();

        // 获取当前时间和过期时间
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenTimeToLive);

        // 生成短令牌值
        String tokenValue = generateShortToken();

        // 创建OAuth2AccessToken
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                issuedAt,
                expiresAt,
                context.getAuthorizedScopes());
    }

    /**
     * 生成16进制格式的令牌值
     * 格式类似: 5a89faa7b4fe1ba7537679c0d7c94039
     */
    private String generateShortToken() {
        byte[] randomBytes = new byte[16]; // 16字节会生成32个16进制字符
        SECURE_RANDOM.nextBytes(randomBytes);

        StringBuilder hexString = new StringBuilder();
        for (byte b : randomBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}