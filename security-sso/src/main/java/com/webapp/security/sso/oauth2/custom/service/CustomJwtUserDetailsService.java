package com.webapp.security.sso.oauth2.custom.service;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义JWT用户详情服务
 * 专门为自定义JWT功能提供用户信息查询
 */
@Service
@ConditionalOnProperty(name = "custom.jwt.enabled", havingValue = "true", matchIfMissing = false)
public class CustomJwtUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomJwtUserDetailsService.class);

    private final SysUserMapper sysUserMapper;

    public CustomJwtUserDetailsService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Custom JWT loading user by username: {}", username);

        // 查询用户信息
        SysUser sysUser = sysUserMapper.selectByUsername(username);

        if (sysUser == null) {
            log.warn("User not found for custom JWT: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查用户状态
        if (sysUser.getStatus() == null || sysUser.getStatus() != 1) {
            log.warn("User is disabled: {}", username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        log.debug("User found for custom JWT: {}", sysUser.getUsername());

        // 获取用户权限列表
        List<String> permissionList = sysUserMapper.selectUserPermissions(sysUser.getUserId());
        log.debug("User permissions: {}", permissionList);

        // 获取用户角色列表
        List<String> roleList = sysUserMapper.selectUserRoles(sysUser.getUserId());
        log.debug("User roles: {}", roleList);

        // 合并权限和角色
        List<String> allAuthorities = permissionList.stream()
                .map(permission -> "PERMISSION_" + permission)
                .collect(Collectors.toList());

        allAuthorities.addAll(roleList.stream()
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toList()));

        // 将权限字符串转换为GrantedAuthority对象
        List<SimpleGrantedAuthority> authorities = allAuthorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        log.debug("User authorities for custom JWT: {}", authorities);

        // 构建UserDetails对象
        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .disabled(false) // 已经在上面检查过状态
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(authorities)
                .build();
    }

    /**
     * 根据用户ID查询用户信息
     */
    public SysUser getUserById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    /**
     * 根据用户名查询用户信息
     */
    public SysUser getUserByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }

    /**
     * 查询用户权限列表
     */
    public List<String> getUserPermissions(Long userId) {
        return sysUserMapper.selectUserPermissions(userId);
    }

    /**
     * 查询用户角色列表
     */
    public List<String> getUserRoles(Long userId) {
        return sysUserMapper.selectUserRoles(userId);
    }
}