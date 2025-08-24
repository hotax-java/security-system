package com.webapp.security.admin.controller.oauth2.dto;

import lombok.Data;

/**
 * PKCE参数响应DTO
 */
@Data
public class PkceParamsDTO {
    
    /**
     * OAuth2 state参数
     */
    private String state;
    
    /**
     * PKCE code_verifier参数
     */
    private String codeVerifier;
    
    /**
     * PKCE code_challenge参数
     */
    private String codeChallenge;
    
    /**
     * PKCE code_challenge_method参数，固定为S256
     */
    private String codeChallengeMethod;
}