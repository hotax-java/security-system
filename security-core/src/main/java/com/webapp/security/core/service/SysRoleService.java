package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.entity.SysRole;

import java.util.List;

/**
 * 系统角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据角色编码查询角色
     */
    SysRole getByCode(String code);

    /**
     * 创建角色
     */
    boolean createRole(SysRole role);

    /**
     * 更新角色信息
     */
    boolean updateRole(SysRole role);

    /**
     * 删除角色（逻辑删除）
     */
    boolean deleteRole(Long roleId);

    /**
     * 启用/禁用角色
     */
    boolean updateRoleStatus(Long roleId, Integer status);

    /**
     * 分配角色权限
     */
    boolean assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色权限列表
     */
    List<String> getRolePermissions(Long roleId);

    /**
     * 获取角色权限对象列表
     */
    List<SysPermission> getRolePermissionVOs(Long roleId);

    /**
     * 根据用户ID获取用户角色列表
     */
    List<SysRole> getUserRoles(Long userId);

    /**
     * 获取所有启用的角色
     */
    List<SysRole> getEnabledRoles();
}
