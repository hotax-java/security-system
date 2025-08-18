package com.webapp.security.core.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.entity.SysUserRole;
import com.webapp.security.core.exception.BizException;
import com.webapp.security.core.mapper.SysUserMapper;
import com.webapp.security.core.mapper.SysUserRoleMapper;
import com.webapp.security.core.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.*;

/**
 * 系统用户服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;

    @Override
    public SysUser getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return baseMapper.selectByUsername(username);
    }

    @Override
    public SysUser getByEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        return baseMapper.selectByEmail(email);
    }

    @Override
    public SysUser getByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        return baseMapper.selectByPhone(phone);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUser(SysUser user) {
        if (user == null || StrUtil.isBlank(user.getUsername())) {
            return false;
        }

        // 检查用户名是否已存在
        if (getByUsername(user.getUsername()) != null) {
            throw UserBizExceptionBuilder.usernameAlreadyExists("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (StrUtil.isNotBlank(user.getEmail()) && getByEmail(user.getEmail()) != null) {
            throw UserBizExceptionBuilder.emailAlreadyExists("邮箱已存在");
        }

        // 检查手机号是否已存在
        if (StrUtil.isNotBlank(user.getPhone()) && getByPhone(user.getPhone()) != null) {
            throw UserBizExceptionBuilder.phoneAlreadyExists("手机号已存在");
        }

        // 加密密码
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        }

        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1); // 默认启用
        user.setDeleted(0); // 默认未删除
        return save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user) {
        if (user == null || user.getUserId() == null) {
            return false;
        }

        SysUser existingUser = getById(user.getUserId());
        if (existingUser == null) {
            throw UserBizExceptionBuilder.notFound(user.getUserId());
        }

        // 检查用户名是否被其他用户使用
        if (StrUtil.isNotBlank(user.getUsername()) && !user.getUsername().equals(existingUser.getUsername())) {
            SysUser userByUsername = getByUsername(user.getUsername());
            if (userByUsername != null && !userByUsername.getUserId().equals(user.getUserId())) {
                throw UserBizExceptionBuilder.usernameAlreadyExists("用户名已被其他用户使用");
            }
        }

        // 检查邮箱是否被其他用户使用
        if (StrUtil.isNotBlank(user.getEmail()) && !user.getEmail().equals(existingUser.getEmail())) {
            SysUser userByEmail = getByEmail(user.getEmail());
            if (userByEmail != null && !userByEmail.getUserId().equals(user.getUserId())) {
                throw UserBizExceptionBuilder.emailAlreadyExists("邮箱已被其他用户使用");
            }
        }

        // 检查手机号是否被其他用户使用
        if (StrUtil.isNotBlank(user.getPhone()) && !user.getPhone().equals(existingUser.getPhone())) {
            SysUser userByPhone = getByPhone(user.getPhone());
            if (userByPhone != null && !userByPhone.getUserId().equals(user.getUserId())) {
                throw UserBizExceptionBuilder.phoneAlreadyExists("手机号已被其他用户使用");
            }
        }

        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        if (userId == null) {
            return false;
        }

        SysUser user = getById(userId);
        if (user == null) {
            return false;
        }

        // 删除用户角色关联
        userRoleMapper.deleteByUserId(userId);

        // 使用MyBatis-Plus的逻辑删除方法
        return removeById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) {
        if (userId == null || StrUtil.isBlank(newPassword)) {
            return false;
        }

        SysUser user = getById(userId);
        if (user == null) {
            return false;
        }

        user.setPassword(BCrypt.hashpw(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            return false;
        }

        SysUser user = getById(userId);
        if (user == null) {
            return false;
        }

        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return false;
        }

        // 删除原有角色关联
        userRoleMapper.deleteByUserId(userId);

        // 添加新的角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            List<List<Long>> partitionedRoleIds = Lists.partition(roleIds, 1000);
            for (List<Long> partitionedRoleId : partitionedRoleIds) {
                List<SysUserRole> userRoles = partitionedRoleId.stream()
                        .map(roleId -> {
                            SysUserRole userRole = new SysUserRole();
                            userRole.setUserId(userId);
                            userRole.setRoleId(roleId);
                            userRole.setCreateTime(LocalDateTime.now());
                            return userRole;
                        })
                        .collect(Collectors.toList());
                int insertCount = userRoleMapper.batchInsert(userRoles);
                if (insertCount != partitionedRoleId.size()) {
                    log.error("批量插入用户角色失败, 用户ID: {}, 批次: {}/{}, 插入条数: {}", userId,
                            (partitionedRoleIds.indexOf(partitionedRoleId) + 1),
                            partitionedRoleIds.size(), insertCount);
                }
            }
        }

        return true;
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return null;
        }
        return baseMapper.selectUserPermissions(userId);
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        if (userId == null) {
            return null;
        }
        return baseMapper.selectUserRoles(userId);
    }

    @Override
    public boolean validatePassword(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return false;
        }

        SysUser user = getByUsername(username);
        if (user == null || user.getStatus() != 1) {
            return false;
        }

        return BCrypt.checkpw(password, user.getPassword());
    }

    private static class UserBizExceptionBuilder {
        public static BizException of(String code, String message) {
            return new BizException("USER_" + code, message);
        }

        public static BizException notFound(Long userId) {
            return of("NOT_FOUND", "用户" + userId + "不存在");
        }

        public static BizException of(String message) {
            return of("INTERNAL_SERVER_ERROR", message);
        }

        public static BizException usernameAlreadyExists(String message) {
            return of("USERNAME_ALREADY_EXISTS", message);
        }

        public static BizException emailAlreadyExists(String message) {
            return of("EMAIL_ALREADY_EXISTS", message);
        }

        public static BizException phoneAlreadyExists(String message) {
            return of("PHONE_ALREADY_EXISTS", message);
        }
    }
}
