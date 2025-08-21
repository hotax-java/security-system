package com.webapp.security.admin.auth.oauth2.model;

import java.io.Serializable;

/**
 * OAuth2 令牌请求模型
 * 用于授权码交换token的请求参数
 */
public class TokenRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String state;
    private String clientId;
    private String codeVerifier; // PKCE的code_verifier参数

    // 默认构造函数
    public TokenRequest() {
    }

    // 带参构造函数
    public TokenRequest(String code, String state, String clientId) {
        this.code = code;
        this.state = state;
        this.clientId = clientId;
    }

    // 带PKCE参数的构造函数
    public TokenRequest(String code, String state, String clientId, String codeVerifier) {
        this.code = code;
        this.state = state;
        this.clientId = clientId;
        this.codeVerifier = codeVerifier;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    @Override
    public String toString() {
        return "TokenRequest{" +
                "code='" + code + '\'' +
                ", state='" + state + '\'' +
                ", clientId='" + clientId + '\'' +
                ", codeVerifier='" + (codeVerifier != null ? "[PROTECTED]" : "null") + '\'' +
                '}';
    }
}