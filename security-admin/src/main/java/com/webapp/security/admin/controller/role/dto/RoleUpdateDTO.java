package com.webapp.security.admin.controller.role.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 角色更新请求DTO
 */
@Data
public class RoleUpdateDTO {
    /**
     * 角色编码
     */
    @Size(min = 2, max = 50, message = "角色编码长度必须在2-50个字符之间")
    private String roleCode;

    /**
     * 角色名称
     */
    @Size(min = 2, max = 50, message = "角色名称长度必须在2-50个字符之间")
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色状态：0-禁用，1-启用
     */
    private Integer status;
}