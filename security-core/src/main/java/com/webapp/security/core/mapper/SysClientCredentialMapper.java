package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysClientCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysClientCredentialMapper extends BaseMapper<SysClientCredential> {

    @Select("SELECT * FROM sys_client_credential WHERE app_id = #{appId} LIMIT 1")
    SysClientCredential findByAppId(@Param("appId") String appId);
}