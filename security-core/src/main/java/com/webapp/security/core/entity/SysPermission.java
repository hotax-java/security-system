package com.webapp.security.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统权限实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_permission")
public class SysPermission {

    /**
     * 权限ID
     */
    @TableId(value = "permission_id", type = IdType.AUTO)
    private Long permissionId;

    /**
     * 权限编码（如：user:add、role:manage）
     */
    @TableField("perm_code")
    private String permCode;

    /**
     * 权限名称
     */
    @TableField("perm_name")
    private String permName;

    /**
     * 权限描述
     */
    @TableField("description")
    private String description;

    /**
     * 权限类型�?-菜单�?-按钮�?-接口
     */
    @TableField("perm_type")
    private Integer permType;

    /**
     * 父权限ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 权限路径
     */
    @TableField("perm_path")
    private String permPath;

    /**
     * 菜单图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 权限状态：0-禁用 1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 删除标志 0-未删除，1-已删
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 子权限列表（用于构建树形结构，不映射到数据库
     */
    @TableField(exist = false)
    private List<SysPermission> children;
}
