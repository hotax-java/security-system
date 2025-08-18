package com.webapp.security.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.entity.SysRole;
import com.webapp.security.core.entity.SysRolePermission;
import com.webapp.security.core.exception.BizException;
import com.webapp.security.core.mapper.SysRoleMapper;
import com.webapp.security.core.mapper.SysRolePermissionMapper;
import com.webapp.security.core.mapper.SysUserRoleMapper;
import com.webapp.security.core.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public SysRole getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return baseMapper.selectByRoleCode(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRole(SysRole role) {
        if (role == null || StrUtil.isBlank(role.getRoleCode())) {
            return false;
        }

        // 检查角色编码是否已存在
        if (getByCode(role.getRoleCode()) != null) {
            throw RoleBizExceptionBuilder.codeAlreadyExists("角色编码已存在");
        }

        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setStatus(1); // 默认启用
        role.setDeleted(0); // 默认未删除
        return save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole role) {
        if (role == null || role.getRoleId() == null) {
            return false;
        }

        SysRole existingRole = getById(role.getRoleId());
        if (existingRole == null) {
            throw RoleBizExceptionBuilder.notFound(role.getRoleId());
        }

        // 检查角色编码是否被其他角色使用
        if (StrUtil.isNotBlank(role.getRoleCode()) && !role.getRoleCode().equals(existingRole.getRoleCode())) {
            SysRole roleByCode = getByCode(role.getRoleCode());
            if (roleByCode != null && !roleByCode.getRoleId().equals(role.getRoleId())) {
                throw RoleBizExceptionBuilder.codeAlreadyExists("角色编码已被其他角色使用");
            }
        }

        role.setUpdateTime(LocalDateTime.now());
        return updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long roleId) {
        if (roleId == null) {
            return false;
        }

        SysRole role = getById(roleId);
        if (role == null) {
            return false;
        }

        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 删除用户角色关联
        userRoleMapper.deleteByRoleId(roleId);

        // 使用MyBatis-Plus的逻辑删除方法
        return removeById(roleId);
    }

    @Override
    public boolean updateRoleStatus(Long roleId, Integer status) {
        if (roleId == null || status == null) {
            return false;
        }

        SysRole role = getById(roleId);
        if (role == null) {
            return false;
        }

        role.setStatus(status);
        role.setUpdateTime(LocalDateTime.now());
        return updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleId == null) {
            return false;
        }

        // 删除原有权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 添加新的权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<List<Long>> partitionedPermissionIds = Lists.partition(permissionIds, 1000);
            for (List<Long> partitionedPermissionId : partitionedPermissionIds) {
                List<SysRolePermission> rolePermissions = partitionedPermissionId.stream()
                        .map(permissionId -> {
                            SysRolePermission rolePermission = new SysRolePermission();
                            rolePermission.setRoleId(roleId);
                            rolePermission.setPermissionId(permissionId);
                            rolePermission.setCreateTime(LocalDateTime.now());
                            return rolePermission;
                        })
                        .collect(Collectors.toList());
                int insertCount = rolePermissionMapper.batchInsert(rolePermissions);
                if (insertCount != partitionedPermissionId.size()) {
                    log.error("批量插入角色权限关联失败, 角色ID: {}, 批次: {}/{}, 插入条数: {}", roleId,
                            (partitionedPermissionIds.indexOf(partitionedPermissionId) + 1),
                            partitionedPermissionIds.size(), insertCount);
                }
            }
        }

        return true;
    }

    @Override
    public List<String> getRolePermissions(Long roleId) {
        if (roleId == null) {
            return null;
        }
        return baseMapper.selectRolePermissions(roleId);
    }

    @Override
    public List<SysPermission> getRolePermissionVOs(Long roleId) {
        if (roleId == null) {
            return null;
        }
        return baseMapper.selectRolePermissionDetails(roleId);
    }

    @Override
    public List<SysRole> getUserRoles(Long userId) {
        if (userId == null) {
            return null;
        }
        return baseMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<SysRole> getEnabledRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1)
                .eq(SysRole::getDeleted, 0)
                .orderByAsc(SysRole::getCreateTime);
        return list(wrapper);
    }

    private static class RoleBizExceptionBuilder {
        public static BizException of(String code, String message) {
            return new BizException("ROLE_" + code, message);
        }

        public static BizException notFound(Long roleId) {
            return of("NOT_FOUND", "角色" + roleId + "不存在");
        }

        public static BizException of(String message) {
            return of("INTERNAL_SERVER_ERROR", message);
        }

        public static BizException codeAlreadyExists(String message) {
            return of("CODE_ALREADY_EXISTS", message);
        }
    }
}
