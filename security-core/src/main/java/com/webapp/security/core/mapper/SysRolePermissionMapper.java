package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联Mapper
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 根据角色ID删除角色权限关联
     * 
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteByRoleId(Long roleId);

    /**
     * 根据权限ID删除角色权限关联
     * 
     * @param permissionId 权限ID
     * @return 影响行数
     */
    int deleteByPermissionId(Long permissionId);

    /**
     * 批量插入角色权限关联
     * 每次最多处理1000条数据，超过需要分批处理
     * 
     * @param rolePermissions 角色权限关联列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<SysRolePermission> rolePermissions);

    /**
     * 根据角色ID查询权限ID列表
     * 
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID查询角色ID列表
     * 
     * @param permissionId 权限ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByPermissionId(@Param("permissionId") Long permissionId);
}
