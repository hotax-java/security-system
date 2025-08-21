package com.webapp.security.sso.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OAuth2客户端注册实体类
 */
@TableName("oauth2_registered_client")
@Data
public class OAuth2RegisteredClient {
    
    @TableId
    private String id;
    
    @TableField("client_id")
    private String clientId;
    
    @TableField("client_id_issued_at")
    private LocalDateTime clientIdIssuedAt;
    
    @TableField("client_secret")
    private String clientSecret;
    
    @TableField("client_secret_expires_at")
    private LocalDateTime clientSecretExpiresAt;
    
    @TableField("client_name")
    private String clientName;
    
    @TableField("client_authentication_methods")
    private String clientAuthenticationMethods;
    
    @TableField("authorization_grant_types")
    private String authorizationGrantTypes;
    
    @TableField("redirect_uris")
    private String redirectUris;

    @TableField("post_logout_redirect_uris")
    private String postLogoutRedirectUris;
    
    @TableField("scopes")
    private String scopes;
    
    @TableField("client_settings")
    private String clientSettings;
    
    @TableField("token_settings")
    private String tokenSettings;
}

