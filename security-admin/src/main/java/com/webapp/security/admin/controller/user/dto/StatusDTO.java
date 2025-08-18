package com.webapp.security.admin.controller.user.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 状态更新请求DTO
 */
@Data
public class StatusDTO {
    /**
     * 状态：0-禁用，1-启用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}