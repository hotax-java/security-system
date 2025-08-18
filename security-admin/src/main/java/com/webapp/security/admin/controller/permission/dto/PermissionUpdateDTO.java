package com.webapp.security.admin.controller.permission.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 权限更新请求DTO
 */
@Data
@MenuPathRequired
public class PermissionUpdateDTO {
    /**
     * 权限编码
     */
    @Size(min = 2, max = 50, message = "权限编码长度必须在2-50个字符之间")
    private String permCode;

    /**
     * 权限名称
     */
    @Size(min = 2, max = 50, message = "权限名称长度必须在2-50个字符之间")
    private String permName;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 权限类型：1-菜单，2-按钮，3-接口
     */
    private Integer permType;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 权限路径
     */
    @Size(max = 2000, message = "权限路径长度不能超过2000个字符")
    private String permPath;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
}