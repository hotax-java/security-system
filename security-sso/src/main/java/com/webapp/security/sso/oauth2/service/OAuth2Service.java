package com.webapp.security.sso.oauth2.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * OAuth2工具类
 * 提供OAuth2相关的公共方法
 */
@Component
public class OAuth2Service {

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final AuthorizationServerSettings authorizationServerSettings;

    public OAuth2Service(
            RegisteredClientRepository registeredClientRepository,
            OAuth2TokenGenerator<?> tokenGenerator,
            AuthorizationServerSettings authorizationServerSettings) {
        this.registeredClientRepository = registeredClientRepository;
        this.tokenGenerator = tokenGenerator;
        this.authorizationServerSettings = authorizationServerSettings;
    }

    /**
     * 根据客户端ID获取注册客户端
     */
    public RegisteredClient getRegisteredClient(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalStateException("客户端ID不能为空");
        }

        RegisteredClient client = registeredClientRepository.findByClientId(clientId.trim());
        if (client == null) {
            throw new IllegalStateException("未找到客户端: " + clientId + "，请确保该客户端已在授权服务器中注册");
        }

        return client;
    }

    /**
     * 创建AuthorizationServerContext
     */
    public AuthorizationServerContext createAuthorizationServerContext() {
        return new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return authorizationServerSettings.getIssuer();
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return authorizationServerSettings;
            }
        };
    }

    /**
     * 生成Access Token - 使用OAuth2TokenContext
     * 统一的令牌生成逻辑，确保JWT格式一致
     */
    public OAuth2AccessToken generateAccessToken(Authentication authentication,
            RegisteredClient registeredClient,
            OAuth2Authorization.Builder authorizationBuilder) {

        // 创建OAuth2TokenContext
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authentication)
                .authorizationServerContext(createAuthorizationServerContext())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizedScopes(registeredClient.getScopes())
                .build();

        // 使用TokenGenerator生成令牌
        OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);
        if (!(generatedToken instanceof Jwt)) {
            throw new IllegalStateException("生成的令牌不是Jwt类型");
        }

        Jwt jwt = (Jwt) generatedToken;

        // 将JWT包装为OAuth2AccessToken
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                registeredClient.getScopes());

        // 将令牌添加到授权构建器
        authorizationBuilder.accessToken(accessToken);

        return accessToken;
    }

    /**
     * 生成Refresh Token
     */
    public OAuth2RefreshToken generateRefreshToken(Authentication authentication,
            RegisteredClient registeredClient,
            OAuth2Authorization.Builder authorizationBuilder) {

        // 检查客户端是否支持refresh token
        if (!registeredClient.getAuthorizationGrantTypes().contains(
                AuthorizationGrantType.REFRESH_TOKEN)) {
            return null;
        }

        // 创建OAuth2TokenContext
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authentication)
                .authorizationServerContext(createAuthorizationServerContext())
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizedScopes(registeredClient.getScopes())
                .build();

        // 使用TokenGenerator生成令牌
        return (OAuth2RefreshToken) tokenGenerator.generate(tokenContext);
    }
}