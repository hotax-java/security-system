package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysUser;

import java.util.List;

/**
 * 系统用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用
     */
    SysUser getByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    SysUser getByEmail(String email);

    /**
     * 根据手机号查询用
     */
    SysUser getByPhone(String phone);

    /**
     * 创建用户
     */
    boolean createUser(SysUser user);

    /**
     * 更新用户信息
     */
    boolean updateUser(SysUser user);

    /**
     * 删除用户（逻辑删除�?     */
    boolean deleteUser(Long userId);

    /**
     * 重置用户密码
     */
    boolean resetPassword(Long userId, String newPassword);

    /**
     * 启用/禁用用户
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 分配用户角色
     */
    boolean assignRoles(Long userId, List<Long> roleIds);

    /**
     * 获取用户权限列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 获取用户角色列表
     */
    List<String> getUserRoles(Long userId);

    /**
     * 验证用户密码
     */
    boolean validatePassword(String username, String password);
}

