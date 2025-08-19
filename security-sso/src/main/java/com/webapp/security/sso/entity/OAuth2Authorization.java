package com.webapp.security.sso.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * OAuth2授权记录实体类
 */
@Data
@TableName("oauth2_authorization")
public class OAuth2Authorization {
    
    @TableId
    private String id;
    
    private String registeredClientId;
    
    private String principalName;
    
    private String authorizationGrantType;
    
    private String authorizedScopes;
    
    private String attributes;
    
    private String state;
    
    // 授权码相关
    private String authorizationCodeValue;
    private Instant authorizationCodeIssuedAt;
    private Instant authorizationCodeExpiresAt;
    private String authorizationCodeMetadata;
    
    // 访问令牌相关
    private String accessTokenValue;
    private Instant accessTokenIssuedAt;
    private Instant accessTokenExpiresAt;
    private String accessTokenMetadata;
    private String accessTokenType;
    private String accessTokenScopes;
    
    // OIDC ID令牌相关
    private String oidcIdTokenValue;
    private Instant oidcIdTokenIssuedAt;
    private Instant oidcIdTokenExpiresAt;
    private String oidcIdTokenMetadata;
    
    // 刷新令牌相关
    private String refreshTokenValue;
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;
    private String refreshTokenMetadata;
}

