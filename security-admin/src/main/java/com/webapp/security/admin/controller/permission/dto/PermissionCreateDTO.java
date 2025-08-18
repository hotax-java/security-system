package com.webapp.security.admin.controller.permission.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 权限创建请求DTO
 */
@Data
@MenuPathRequired
public class PermissionCreateDTO {
    /**
     * 权限编码
     */
    @NotBlank(message = "权限编码不能为空")
    @Size(min = 2, max = 50, message = "权限编码长度必须在2-50个字符之间")
    private String permCode;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    @Size(min = 2, max = 50, message = "权限名称长度必须在2-50个字符之间")
    private String permName;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 权限类型：1-菜单，2-按钮，3-接口
     */
    @NotNull(message = "权限类型不能为空")
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
    private Integer sortOrder = 1;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status = 1;
}