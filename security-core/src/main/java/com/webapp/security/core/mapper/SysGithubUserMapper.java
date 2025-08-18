package com.webapp.security.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.core.entity.SysGithubUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * GitHub用户Mapper接口
 */
@Mapper
public interface SysGithubUserMapper extends BaseMapper<SysGithubUser> {
}