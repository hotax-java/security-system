package com.webapp.security.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.exception.BizException;
import com.webapp.security.core.mapper.SysPermissionMapper;
import com.webapp.security.core.mapper.SysRolePermissionMapper;
import com.webapp.security.core.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统权限服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public SysPermission getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return baseMapper.selectByPermCode(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createPermission(SysPermission permission) {
        if (permission == null || StrUtil.isBlank(permission.getPermCode())) {
            return false;
        }

        // 检查权限编码是否已存在
        if (getByCode(permission.getPermCode()) != null) {
            throw PermissionBizExceptionBuilder.codeAlreadyExists("权限编码已存在");
        }

        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());
        permission.setStatus(1); // 默认启用
        permission.setDeleted(0); // 默认未删除
        return save(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePermission(SysPermission permission) {
        if (permission == null || permission.getPermissionId() == null) {
            return false;
        }

        SysPermission existingPermission = getById(permission.getPermissionId());
        if (existingPermission == null) {
            throw PermissionBizExceptionBuilder.notFound(permission.getPermissionId());
        }

        // 检查权限编码是否被其他权限使用
        if (StrUtil.isNotBlank(permission.getPermCode())
                && !permission.getPermCode().equals(existingPermission.getPermCode())) {
            SysPermission permissionByCode = getByCode(permission.getPermCode());
            if (permissionByCode != null && !permissionByCode.getPermissionId().equals(permission.getPermissionId())) {
                throw PermissionBizExceptionBuilder.codeAlreadyExists("权限编码已被其他权限使用");
            }
        }

        permission.setUpdateTime(LocalDateTime.now());
        return updateById(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePermission(Long permissionId) {
        if (permissionId == null) {
            return false;
        }

        SysPermission permission = getById(permissionId);
        if (permission == null) {
            return false;
        }

        // 检查是否有子权限
        List<SysPermission> childPermissions = getChildPermissions(permissionId);
        if (!childPermissions.isEmpty()) {
            throw PermissionBizExceptionBuilder.hasChildren("存在子权限，无法删除");
        }

        // 删除角色权限关联
        rolePermissionMapper.deleteByPermissionId(permissionId);

        // 使用MyBatis-Plus的逻辑删除方法
        return removeById(permissionId);
    }

    @Override
    public boolean updatePermissionStatus(Long permissionId, Integer status) {
        if (permissionId == null || status == null) {
            return false;
        }

        SysPermission permission = getById(permissionId);
        if (permission == null) {
            return false;
        }

        permission.setStatus(status);
        permission.setUpdateTime(LocalDateTime.now());
        return updateById(permission);
    }

    @Override
    public List<SysPermission> getChildPermissions(Long parentId) {
        if (parentId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByParentId(parentId);
    }

    @Override
    public List<SysPermission> getMenuPermissions() {
        List<SysPermission> allMenus = baseMapper.selectByPermType(1); // 1表示菜单类型
        return buildPermissionTree(allMenus);
    }

    @Override
    public List<SysPermission> getPermissionsByType(Integer permType) {
        List<SysPermission> permissions = baseMapper.selectByPermType(permType);
        return buildPermissionTree(permissions);
    }

    @Override
    public List<SysPermission> getRolePermissions(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByRoleId(roleId);
    }

    @Override
    public List<SysPermission> getUserPermissions(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public List<SysPermission> getEnabledPermissions() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getStatus, 1)
                .eq(SysPermission::getDeleted, 0)
                .orderByAsc(SysPermission::getSortOrder);
        return list(wrapper);
    }

    @Override
    public List<SysPermission> buildPermissionTree(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // 按父ID分组
        Map<Long, List<SysPermission>> permissionMap = permissions.stream()
                .collect(Collectors
                        .groupingBy(permission -> permission.getParentId() == null ? 0L : permission.getParentId()));

        // 构建树形结构
        List<SysPermission> rootPermissions = permissionMap.getOrDefault(0L, new ArrayList<>());
        buildChildren(rootPermissions, permissionMap);

        return rootPermissions;
    }

    /**
     * 递归构建子权限
     */
    private void buildChildren(List<SysPermission> permissions, Map<Long, List<SysPermission>> permissionMap) {
        for (SysPermission permission : permissions) {
            List<SysPermission> children = permissionMap.getOrDefault(permission.getPermissionId(), new ArrayList<>());
            if (!children.isEmpty()) {
                permission.setChildren(children);
                buildChildren(children, permissionMap);
            }
        }
    }

    private static class PermissionBizExceptionBuilder {
        public static BizException of(String code, String message) {
            return new BizException("PERMISSION_" + code, message);
        }

        public static BizException notFound(Long permissionId) {
            return of("NOT_FOUND", "权限" + permissionId + "不存在");
        }

        public static BizException of(String message) {
            return of("INTERNAL_SERVER_ERROR", message);
        }

        public static BizException codeAlreadyExists(String message) {
            return of("CODE_ALREADY_EXISTS", message);
        }

        public static BizException hasChildren(String message) {
            return of("HAS_CHILDREN", message);
        }
    }
}
