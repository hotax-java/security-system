package com.webapp.security.admin.controller.sysclientcredential.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CredentialCreateResultDTO {
    private String appId;
    private String appSecret;
}