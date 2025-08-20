package com.webapp.security.core.auths.oauth2.model;

import java.io.Serializable;

/**
 * OAuth2 令牌请求模型
 * 用于授权码交换token的请求参数
 */
public class TokenRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String state;
    private String redirectUri;
    private String clientId;

    // 默认构造函数
    public TokenRequest() {
    }

    // 带参构造函数
    public TokenRequest(String code, String state, String redirectUri, String clientId) {
        this.code = code;
        this.state = state;
        this.redirectUri = redirectUri;
        this.clientId = clientId;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        return "TokenRequest{" +
                "code='" + code + '\'' +
                ", state='" + state + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}