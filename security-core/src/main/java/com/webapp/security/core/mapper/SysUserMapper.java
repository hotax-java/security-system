package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统用户Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

        /**
         * 根据用户名查询用户
         *
         * @param username 用户名
         * @return 用户信息
         */
        SysUser selectByUsername(@Param("username") String username);

        /**
         * 根据邮箱查询用户
         *
         * @param email 邮箱
         * @return 用户信息
         */
        SysUser selectByEmail(@Param("email") String email);

        /**
         * 根据手机号查询用户
         *
         * @param phone 手机号
         * @return 用户信息
         */
        SysUser selectByPhone(@Param("phone") String phone);

        /**
         * 查询用户的权限编码列表
         *
         * @param userId 用户ID
         * @return 权限编码列表
         */
        List<String> selectUserPermissions(@Param("userId") Long userId);

        /**
         * 查询用户的角色编码列表
         *
         * @param userId 用户ID
         * @return 角色编码列表
         */
        List<String> selectUserRoles(@Param("userId") Long userId);
}
