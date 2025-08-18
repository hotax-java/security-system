package com.webapp.security.sso.oauth2.expand;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户详情服务实现
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        // 查询用户信息
        SysUser sysUser = sysUserMapper.selectByUsername(username);

        if (sysUser == null) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        log.debug("User found: {}", sysUser.getUsername());

        // 获取用户权限列表
        List<String> permissionList = sysUserMapper.selectUserPermissions(sysUser.getUserId());

        // 将权限字符串转换为GrantedAuthority对象
        List<SimpleGrantedAuthority> authorities = permissionList.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        log.debug("User authorities: {}", authorities);

        // 构建UserDetails对象
        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .disabled(sysUser.getStatus() == null || sysUser.getStatus() != 1) // status=1表示启用
                .accountExpired(false) // 暂时设为false
                .accountLocked(false) // 暂时设为false
                .credentialsExpired(false) // 暂时设为false
                .authorities(authorities) // 设置用户权限
                .build();
    }

}
