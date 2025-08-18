package com.webapp.security.admin.controller.sysclientcredential.vo;

import lombok.Data;

@Data
public class CredentialVO {
    private Long id;
    private String appId;
    private String clientId;
    private Integer status;
    private String createBy;
}