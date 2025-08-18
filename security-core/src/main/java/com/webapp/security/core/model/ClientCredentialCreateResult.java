package com.webapp.security.core.model;

import com.webapp.security.core.entity.SysClientCredential;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientCredentialCreateResult {
    private String appId;
    private String plainSecret;
    private SysClientCredential entity;
}