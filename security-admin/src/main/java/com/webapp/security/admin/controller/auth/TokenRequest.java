package com.webapp.security.admin.controller.auth;

import lombok.Data;

/**
 * Token请求参数类
 */
@Data
public class TokenRequest {
    private String code;
    private String state;
    private String redirectUri;
    private String clientId;
}