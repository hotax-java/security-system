package com.webapp.security.admin.controller.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 菜单VO
 */
@Data
public class MenuVO {
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
     * 权限类型
     */
    private Integer permType;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 权限路径
     */
    private String permPath;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 权限状态
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 子菜单列表
     */
    private List<MenuVO> children;
}