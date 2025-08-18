package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统权限Mapper
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

        /**
         * 根据权限编码查询权限
         *
         * @param permCode 权限编码
         * @return 权限信息
         */
        SysPermission selectByPermCode(@Param("permCode") String permCode);

        /**
         * 根据父权限ID查询子权限列表
         *
         * @param parentId 父权限ID
         * @return 子权限列表
         */
        List<SysPermission> selectByParentId(@Param("parentId") Long parentId);

        /**
         * 查询所有启用的权限
         *
         * @return 权限列表
         */
        List<SysPermission> selectAllEnabled();

        /**
         * 根据权限类型查询权限列表
         *
         * @param permType 权限类型
         * @return 权限列表
         */
        List<SysPermission> selectByPermType(@Param("permType") Integer permType);

        /**
         * 根据角色ID查询权限列表
         *
         * @param roleId 角色ID
         * @return 权限列表
         */
        List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

        /**
         * 根据用户ID查询权限列表
         *
         * @param userId 用户ID
         * @return 权限列表
         */
        List<SysPermission> selectByUserId(@Param("userId") Long userId);
}
