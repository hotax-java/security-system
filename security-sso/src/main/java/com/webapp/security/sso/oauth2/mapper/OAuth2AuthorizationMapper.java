package com.webapp.security.sso.oauth2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.sso.oauth2.entity.OAuth2Authorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * OAuth2授权记录Mapper
 */
@Mapper
public interface OAuth2AuthorizationMapper extends BaseMapper<OAuth2Authorization> {

    /**
     * 根据访问令牌查找授权记录
     */
    @Select("SELECT * FROM oauth2_authorization WHERE access_token_value = #{tokenValue}")
    OAuth2Authorization findByAccessToken(@Param("tokenValue") String tokenValue);

    /**
     * 根据刷新令牌查找授权记录
     */
    @Select("SELECT * FROM oauth2_authorization WHERE refresh_token_value = #{tokenValue}")
    OAuth2Authorization findByRefreshToken(@Param("tokenValue") String tokenValue);

    /**
     * 根据授权码查找授权记
     */
    @Select("SELECT * FROM oauth2_authorization WHERE authorization_code_value = #{codeValue}")
    OAuth2Authorization findByAuthorizationCode(@Param("codeValue") String codeValue);
}
