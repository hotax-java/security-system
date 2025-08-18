package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统角色Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

        /**
         * 根据角色编码查询角色
         *
         * @param roleCode 角色编码
         * @return 角色信息
         */
        SysRole selectByRoleCode(@Param("roleCode") String roleCode);

        /**
         * 查询角色的权限编码列表
         *
         * @param roleId 角色ID
         * @return 权限编码列表
         */
        List<String> selectRolePermissions(@Param("roleId") Long roleId);

        /**
         * 查询角色的权限完整信息
         *
         * @param roleId 角色ID
         * @return 权限信息列表
         */
        List<SysPermission> selectRolePermissionDetails(@Param("roleId") Long roleId);

        /**
         * 根据用户ID查询用户的角色列表
         *
         * @param userId 用户ID
         * @return 角色列表
         */
        List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
