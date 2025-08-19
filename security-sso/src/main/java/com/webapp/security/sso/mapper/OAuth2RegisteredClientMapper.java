package com.webapp.security.sso.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.sso.entity.OAuth2RegisteredClient;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth2客户端注册Mapper接口
 */
@Mapper
public interface OAuth2RegisteredClientMapper extends BaseMapper<OAuth2RegisteredClient> {
}

