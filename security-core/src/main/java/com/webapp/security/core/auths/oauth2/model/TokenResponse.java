package com.webapp.security.core.auths.oauth2.model;

import java.io.Serializable;

/**
 * OAuth2 令牌响应模型
 * 用于返回OAuth2服务器的令牌响应
 */
public class TokenResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String access_token;
    private String token_type;
    private String refresh_token;
    private long expires_in;
    private String scope;

    // 默认构造函数
    public TokenResponse() {
    }

    // 带参构造函数
    public TokenResponse(String access_token, String token_type, String refresh_token, long expires_in, String scope) {
        this.access_token = access_token;
        this.token_type = token_type;
        this.refresh_token = refresh_token;
        this.expires_in = expires_in;
        this.scope = scope;
    }

    // Getters and Setters
    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
                "access_token='" + access_token + '\'' +
                ", token_type='" + token_type + '\'' +
                ", refresh_token='" + (refresh_token != null ? "******" : null) + '\'' +
                ", expires_in=" + expires_in +
                ", scope='" + scope + '\'' +
                '}';
    }
}