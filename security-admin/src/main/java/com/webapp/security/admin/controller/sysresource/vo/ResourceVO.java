package com.webapp.security.admin.controller.sysresource.vo;

import lombok.Data;

@Data
public class ResourceVO {
    private Long resourceId;
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