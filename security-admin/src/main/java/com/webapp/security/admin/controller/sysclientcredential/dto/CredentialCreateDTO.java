package com.webapp.security.admin.controller.sysclientcredential.dto;

import lombok.Data;

/**
 * 客户端凭证创建DTO
 */
@Data
public class CredentialCreateDTO {
    /**
     * 应用ID（前端生成模式使用）
     */
    private String appId;

    /**
     * 应用密钥明文（前端生成模式使用）
     */
    private String appSecret;

    /**
     * 备注
     */
    private String remark;
}