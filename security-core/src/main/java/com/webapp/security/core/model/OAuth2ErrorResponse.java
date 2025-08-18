package com.webapp.security.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 标准错误响应类
 * 统一处理OAuth2相关的错误响应格式
 * 
 * 符合RFC 6749 OAuth2规范的错误响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    // OAuth2 标准错误码常量
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String INVALID_SCOPE = "invalid_scope";
    public static final String SERVER_ERROR = "server_error";
    public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";

    /**
     * OAuth2标准错误码
     */
    private String error;

    /**
     * 错误描述
     */
    private String error_description;

    /**
     * 错误详细信息URI（可选）
     */
    private String error_uri;

    /**
     * 状态参数（授权码流程中使用）
     */
    private String state;

    /**
     * 是否成功
     */
    @Builder.Default
    private Boolean success = false;

    /**
     * 创建成功响应
     */
    public static ResponseEntity<OAuth2ErrorResponse> ok(String description) {
        OAuth2ErrorResponse response = OAuth2ErrorResponse.builder()
                .success(true)
                .error_description(description)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * 创建错误响应
     */
    public static ResponseEntity<OAuth2ErrorResponse> error(String errorCode, String description, HttpStatus status) {
        OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
                .error(errorCode)
                .error_description(description)
                .success(false)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}