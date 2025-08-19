package com.webapp.security.sso.auths.oauth2.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.stereotype.Service;


/**
 * OAuth2工具类
 * 提供OAuth2相关的公共方法
 */
@Service
public class OAuth2Service {

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final AuthorizationServerSettings authorizationServerSettings;

    @Autowired
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
                .authorizationGrantType(new AuthorizationGrantType("password"))
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
                .authorizationGrantType(new AuthorizationGrantType("password"))
                .authorizedScopes(registeredClient.getScopes())
                .build();

        // 使用TokenGenerator生成令牌
        OAuth2RefreshToken generate = (OAuth2RefreshToken) tokenGenerator.generate(tokenContext);
        // 将令牌添加到授权构建器
        authorizationBuilder.refreshToken(generate);
        return generate;
    }

    /********** OAuth2 授权码流程相关方法 **********/
    
    /**
     * 生成OAuth2授权码
     */
    public OAuth2AuthorizationCode generateAuthorizationCode(Authentication authentication,
            RegisteredClient registeredClient,
            OAuth2Authorization.Builder authorizationBuilder) {

        // 创建OAuth2TokenContext
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(authentication)
                .authorizationServerContext(createAuthorizationServerContext())
                .tokenType(new OAuth2TokenType(OAuth2ParameterNames.CODE))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(registeredClient.getScopes())
                .build();

        // 使用TokenGenerator生成授权码
        OAuth2AuthorizationCode authorizationCode = (OAuth2AuthorizationCode) tokenGenerator.generate(tokenContext);
        
        // 将授权码添加到授权构建器
        authorizationBuilder.token(authorizationCode);
        
        return authorizationCode;
    }
}