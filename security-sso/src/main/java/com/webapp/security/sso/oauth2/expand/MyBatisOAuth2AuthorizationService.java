package com.webapp.security.sso.oauth2.expand;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.sso.oauth2.entity.OAuth2Authorization;
import com.webapp.security.sso.oauth2.mapper.OAuth2AuthorizationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 基于MyBatis的OAuth2AuthorizationService实现
 */
@Slf4j
public class MyBatisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final OAuth2AuthorizationMapper authorizationMapper;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    public MyBatisOAuth2AuthorizationService(OAuth2AuthorizationMapper authorizationMapper,
            RegisteredClientRepository registeredClientRepository) {
        this.authorizationMapper = authorizationMapper;
        this.registeredClientRepository = registeredClientRepository;
        this.objectMapper = new ObjectMapper();

        ClassLoader classLoader = MyBatisOAuth2AuthorizationService.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        this.objectMapper.registerModules(securityModules);
        this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }

    @Override
    public void save(org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        OAuth2Authorization entity = toEntity(authorization);

        // 检查是否已存在
        OAuth2Authorization existing = authorizationMapper.selectById(authorization.getId());
        if (existing != null) {
            authorizationMapper.updateById(entity);
        } else {
            authorizationMapper.insert(entity);
        }
    }

    @Override
    public void remove(org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        authorizationMapper.deleteById(authorization.getId());
    }

    @Override
    public org.springframework.security.oauth2.server.authorization.OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        OAuth2Authorization entity = authorizationMapper.selectById(id);
        return entity != null ? toObject(entity) : null;
    }

    @Override
    public org.springframework.security.oauth2.server.authorization.OAuth2Authorization findByToken(String token,
            OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");

        OAuth2Authorization entity = null;

        if (tokenType == null) {
            // 尝试所有令牌类型
            entity = authorizationMapper.findByAccessToken(token);
            if (entity == null) {
                entity = authorizationMapper.findByRefreshToken(token);
            }
            if (entity == null) {
                entity = authorizationMapper.findByAuthorizationCode(token);
            }
        } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            entity = authorizationMapper.findByAccessToken(token);
        } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            entity = authorizationMapper.findByRefreshToken(token);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            entity = authorizationMapper.findByAuthorizationCode(token);
        }

        return entity != null ? toObject(entity) : null;
    }

    /**
     * 将OAuth2Authorization转换为实体类
     */
    private OAuth2Authorization toEntity(
            org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization) {
        OAuth2Authorization entity = new OAuth2Authorization();
        entity.setId(authorization.getId());
        entity.setRegisteredClientId(authorization.getRegisteredClientId());
        entity.setPrincipalName(authorization.getPrincipalName());
        entity.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
        entity.setAuthorizedScopes(StringUtils.collectionToCommaDelimitedString(authorization.getAuthorizedScopes()));
        entity.setAttributes(writeMap(authorization.getAttributes()));
        entity.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));

        // 授权码
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode = authorization
                .getToken(OAuth2AuthorizationCode.class);
        setTokenValues(entity, authorizationCode,
                entity::setAuthorizationCodeValue,
                entity::setAuthorizationCodeIssuedAt,
                entity::setAuthorizationCodeExpiresAt,
                entity::setAuthorizationCodeMetadata);

        // 访问令牌
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization
                .getToken(OAuth2AccessToken.class);
        setTokenValues(entity, accessToken,
                entity::setAccessTokenValue,
                entity::setAccessTokenIssuedAt,
                entity::setAccessTokenExpiresAt,
                entity::setAccessTokenMetadata);
        if (accessToken != null && accessToken.getToken().getTokenType() != null) {
            entity.setAccessTokenType(accessToken.getToken().getTokenType().getValue());
            entity.setAccessTokenScopes(
                    StringUtils.collectionToCommaDelimitedString(accessToken.getToken().getScopes()));
        }

        // OIDC ID令牌
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token<OidcIdToken> oidcIdToken = authorization
                .getToken(OidcIdToken.class);
        setTokenValues(entity, oidcIdToken,
                entity::setOidcIdTokenValue,
                entity::setOidcIdTokenIssuedAt,
                entity::setOidcIdTokenExpiresAt,
                entity::setOidcIdTokenMetadata);

        // 刷新令牌
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization
                .getToken(OAuth2RefreshToken.class);
        setTokenValues(entity, refreshToken,
                entity::setRefreshTokenValue,
                entity::setRefreshTokenIssuedAt,
                entity::setRefreshTokenExpiresAt,
                entity::setRefreshTokenMetadata);

        return entity;
    }

    /**
     * 将实体类转换为OAuth2Authorization
     */
    private org.springframework.security.oauth2.server.authorization.OAuth2Authorization toObject(
            OAuth2Authorization entity) {
        RegisteredClient registeredClient = this.registeredClientRepository.findById(entity.getRegisteredClientId());
        if (registeredClient == null) {
            throw new DataRetrievalFailureException(
                    "The RegisteredClient with id '" + entity.getRegisteredClientId()
                            + "' was not found in the RegisteredClientRepository.");
        }

        org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Builder builder = org.springframework.security.oauth2.server.authorization.OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .id(entity.getId())
                .principalName(entity.getPrincipalName())
                .authorizationGrantType(resolveAuthorizationGrantType(entity.getAuthorizationGrantType()))
                .authorizedScopes(StringUtils.commaDelimitedListToSet(entity.getAuthorizedScopes()))
                .attributes(attributes -> attributes.putAll(parseMap(entity.getAttributes())));

        if (entity.getState() != null) {
            builder.attribute(OAuth2ParameterNames.STATE, entity.getState());
        }

        // 授权码
        if (entity.getAuthorizationCodeValue() != null) {
            OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                    entity.getAuthorizationCodeValue(),
                    entity.getAuthorizationCodeIssuedAt(),
                    entity.getAuthorizationCodeExpiresAt());
            builder.token(authorizationCode,
                    metadata -> metadata.putAll(parseMap(entity.getAuthorizationCodeMetadata())));
        }

        // 访问令牌
        if (entity.getAccessTokenValue() != null) {
            OAuth2AccessToken.TokenType tokenType = null;
            if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(entity.getAccessTokenType())) {
                tokenType = OAuth2AccessToken.TokenType.BEARER;
            }
            Set<String> scopes = StringUtils.commaDelimitedListToSet(entity.getAccessTokenScopes());
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    tokenType,
                    entity.getAccessTokenValue(),
                    entity.getAccessTokenIssuedAt(),
                    entity.getAccessTokenExpiresAt(),
                    scopes);
            builder.token(accessToken, metadata -> metadata.putAll(parseMap(entity.getAccessTokenMetadata())));
        }

        // OIDC ID令牌
        if (entity.getOidcIdTokenValue() != null) {
            OidcIdToken idToken = new OidcIdToken(
                    entity.getOidcIdTokenValue(),
                    entity.getOidcIdTokenIssuedAt(),
                    entity.getOidcIdTokenExpiresAt(),
                    parseMap(entity.getOidcIdTokenMetadata()));
            builder.token(idToken, metadata -> metadata.putAll(parseMap(entity.getOidcIdTokenMetadata())));
        }

        // 刷新令牌
        if (entity.getRefreshTokenValue() != null) {
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                    entity.getRefreshTokenValue(),
                    entity.getRefreshTokenIssuedAt(),
                    entity.getRefreshTokenExpiresAt());
            builder.token(refreshToken, metadata -> metadata.putAll(parseMap(entity.getRefreshTokenMetadata())));
        }

        return builder.build();
    }

    private void setTokenValues(OAuth2Authorization entity,
            org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token<?> token,
            Consumer<String> tokenValueConsumer,
            Consumer<Instant> issuedAtConsumer,
            Consumer<Instant> expiresAtConsumer,
            Consumer<String> metadataConsumer) {
        if (token != null) {
            OAuth2Token oAuth2Token = token.getToken();
            tokenValueConsumer.accept(oAuth2Token.getTokenValue());
            issuedAtConsumer.accept(oAuth2Token.getIssuedAt());
            expiresAtConsumer.accept(oAuth2Token.getExpiresAt());
            metadataConsumer.accept(writeMap(token.getMetadata()));
        }
    }

    private AuthorizationGrantType resolveAuthorizationGrantType(String authorizationGrantType) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        }
        return new AuthorizationGrantType(authorizationGrantType);
    }

    private Map<String, Object> parseMap(String data) {
        try {
            return this.objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private String writeMap(Map<String, Object> data) {
        try {
            return this.objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}
