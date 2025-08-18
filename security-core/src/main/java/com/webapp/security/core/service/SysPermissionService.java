package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysPermission;

import java.util.List;

/**
 * 系统权限服务接口
 */
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 根据权限编码查询权限
     */
    SysPermission getByCode(String code);

    /**
     * 创建权限
     */
    boolean createPermission(SysPermission permission);

    /**
     * 更新权限信息
     */
    boolean updatePermission(SysPermission permission);

    /**
     * 删除权限（逻辑删除）
     */
    boolean deletePermission(Long permissionId);

    /**
     * 启用/禁用权限
     */
    boolean updatePermissionStatus(Long permissionId, Integer status);

    /**
     * 根据父权限ID获取子权限列表
     */
    List<SysPermission> getChildPermissions(Long parentId);

    /**
     * 获取所有菜单权限（树形结构）
     */
    List<SysPermission> getMenuPermissions();

    /**
     * 根据权限类型获取权限列表（树形结构）
     */
    List<SysPermission> getPermissionsByType(Integer permType);

    /**
     * 根据角色ID获取权限列表
     */
    List<SysPermission> getRolePermissions(Long roleId);

    /**
     * 根据用户ID获取权限列表
     */
    List<SysPermission> getUserPermissions(Long userId);

    /**
     * 获取所有启用的权限
     */
    List<SysPermission> getEnabledPermissions();

    /**
     * 构建权限树
     */
    List<SysPermission> buildPermissionTree(List<SysPermission> permissions);
}
