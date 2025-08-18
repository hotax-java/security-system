package com.webapp.security.admin.facade;

import com.webapp.security.admin.controller.dashboard.vo.DashboardStatVO;
import com.webapp.security.admin.controller.dashboard.vo.MenuVO;
import com.webapp.security.admin.converter.UserConverter;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysPermissionService;
import com.webapp.security.core.service.SysRoleService;
import com.webapp.security.core.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘数据聚合服务
 */
@Service
@RequiredArgsConstructor
public class DashboardFacade {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysPermissionService permissionService;
    private final UserConverter userConverter;

    /**
     * 获取仪表盘统计数据
     */
    public DashboardStatVO getDashboardStats() {
        DashboardStatVO stats = new DashboardStatVO();

        // 获取用户总数
        stats.setTotalUsers(userService.count());

        // 获取活跃用户数（状态为启用的用户）
        stats.setActiveUsers(countActiveUsers());

        // 获取在线用户数（暂时设置为活跃用户数的一半，后续可以通过Redis实现）
        stats.setOnlineUsers(countOnlineUsers());

        // 获取角色总数
        stats.setTotalRoles(roleService.count());

        // 获取权限总数
        stats.setTotalPermissions(permissionService.count());

        // 获取最近7天新增的用户
        List<SysUser> recentUsers = getRecentUsers(7);
        stats.setRecentUsers(userConverter.toVOList(recentUsers));

        return stats;
    }

    /**
     * 获取当前用户的菜单权限
     */
    public List<MenuVO> getCurrentUserMenus() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ArrayList<>();
        }

        String username = authentication.getName();
        SysUser currentUser = userService.getByUsername(username);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        // 获取用户的所有权限（包括菜单类型）
        List<SysPermission> userPermissions = permissionService.getUserPermissions(currentUser.getUserId());

        // 过滤出菜单类型的权限（perm_type = 1）
        List<SysPermission> menuPermissions = userPermissions.stream()
                .filter(permission -> permission.getPermType() != null && permission.getPermType() == 1)
                .collect(Collectors.toList());

        // 构建菜单树形结构
        List<SysPermission> menuTree = permissionService.buildPermissionTree(menuPermissions);

        // 转换为MenuVO
        return convertToMenuVO(menuTree);
    }

    /**
     * 统计活跃用户数量
     */
    private long countActiveUsers() {
        return userService.list().stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .count();
    }

    /**
     * 统计在线用户数量
     */
    private long countOnlineUsers() {
        // 假设在线用户数是活跃用户数的一半
        return countActiveUsers() / 2;
    }

    /**
     * 获取最近几天新增的用户
     */
    private List<SysUser> getRecentUsers(int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return userService.list().stream()
                .filter(user -> user.getCreateTime() != null && user.getCreateTime().isAfter(startDate))
                .limit(10) // 最多返回10个用户
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 将权限实体转换为菜单VO
     */
    private List<MenuVO> convertToMenuVO(List<SysPermission> permissions) {
        return permissions.stream()
                .map(this::convertToMenuVO)
                .collect(Collectors.toList());
    }

    /**
     * 将单个权限实体转换为菜单VO
     */
    private MenuVO convertToMenuVO(SysPermission permission) {
        MenuVO menuVO = new MenuVO();
        menuVO.setPermissionId(permission.getPermissionId());
        menuVO.setPermCode(permission.getPermCode());
        menuVO.setPermName(permission.getPermName());
        menuVO.setDescription(permission.getDescription());
        menuVO.setPermType(permission.getPermType());
        menuVO.setParentId(permission.getParentId());
        menuVO.setPermPath(permission.getPermPath());
        menuVO.setIcon(permission.getIcon());
        menuVO.setStatus(permission.getStatus());
        menuVO.setSortOrder(permission.getSortOrder());

        // 递归转换子菜单
        if (permission.getChildren() != null && !permission.getChildren().isEmpty()) {
            List<MenuVO> children = permission.getChildren().stream()
                    .map(this::convertToMenuVO)
                    .collect(Collectors.toList());
            menuVO.setChildren(children);
        }

        return menuVO;
    }
}