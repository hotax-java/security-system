package com.webapp.security.admin.controller.permission.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限信息VO
 */
@Data
public class PermissionVO {
    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 权限编码
     */
    private String permCode;

    /**
     * 权限名称
     */
    private String permName;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 权限类型：0-菜单，1-按钮，2-接口
     */
    private Integer permType;

    /**
     * 权限类型名称
     */
    private String permTypeName;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 父权限名称
     */
    private String parentName;

    /**
     * 权限路径
     */
    private String permPath;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 子权限列表
     */
    private List<PermissionVO> children;
}