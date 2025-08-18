package com.webapp.security.sso.oauth2.model;

/**
 * 登出请求DTO
 */
public class LogoutRequest {
    private String accessToken;
    private String refreshToken;
    
    // 构造函
    public LogoutRequest() {}
    
    public LogoutRequest(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public LogoutRequest(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    @Override
    public String toString() {
        return "LogoutRequest{" +
                "accessToken='[PROTECTED]'" +
                ", refreshToken='[PROTECTED]'" +
                '}';
    }
}

