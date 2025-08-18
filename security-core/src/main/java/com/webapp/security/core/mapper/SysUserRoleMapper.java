package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 根据用户ID删除用户角色关联
     * 
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(Long userId);

    /**
     * 根据角色ID删除用户角色关联
     * 
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteByRoleId(Long roleId);

    /**
     * 批量插入用户角色关联
     * 每次最多处理1000条数据，超过需要分批处理
     * 
     * @param userRoles 用户角色关联列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<SysUserRole> userRoles);

    /**
     * 根据用户ID查询角色ID列表
     * 
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询用户ID列表
     * 
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
