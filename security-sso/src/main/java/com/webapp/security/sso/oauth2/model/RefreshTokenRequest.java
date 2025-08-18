package com.webapp.security.sso.oauth2.model;

/**
 * 刷新令牌请求DTO
 */
public class RefreshTokenRequest {
    private String refreshToken;
    private String clientId;
    
    // 构造函
    public RefreshTokenRequest() {}
    
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public RefreshTokenRequest(String refreshToken, String clientId) {
        this.refreshToken = refreshToken;
        this.clientId = clientId;
    }
    
    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    @Override
    public String toString() {
        return "RefreshTokenRequest{" +
                "refreshToken='[PROTECTED]'" +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}

