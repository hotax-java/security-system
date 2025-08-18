package com.webapp.security.admin.controller.sysresource.dto;

import lombok.Data;

@Data
public class ResourceDTO {
    private String resourceCode;
    private String resourceName;
    private String resourcePath;
    private String method;
    private Integer qpsLimit;
    private Integer burstCapacity;
    private Integer dailyQuota;
    private Integer concurrencyLimit;
    private Integer status;
}