package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysCredentialResourceRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysCredentialResourceRelMapper extends BaseMapper<SysCredentialResourceRel> {

    @Select("SELECT EXISTS(SELECT 1 FROM sys_credential_resource_rel WHERE credential_id = #{credentialId} AND resource_id = #{resourceId})")
    boolean exists(@Param("credentialId") Long credentialId, @Param("resourceId") Long resourceId);
}