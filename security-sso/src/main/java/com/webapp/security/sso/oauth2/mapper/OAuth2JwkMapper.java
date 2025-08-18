package com.webapp.security.sso.oauth2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.webapp.security.sso.oauth2.entity.OAuth2Jwk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OAuth2 JWK密钥Mapper接口
 */
@Mapper
public interface OAuth2JwkMapper extends BaseMapper<OAuth2Jwk> {
    
    /**
     * 查找当前有效的JWK
     */
    @Select("SELECT * FROM oauth2_jwk WHERE is_active = 1 AND expires_at > NOW() ORDER BY created_time DESC LIMIT 1")
    OAuth2Jwk findActiveJwk();
    
    /**
     * 查找所有有效的JWK（用于JWK Set�?     */
    @Select("SELECT * FROM oauth2_jwk WHERE is_active = 1 AND expires_at > NOW() ORDER BY created_time DESC")
    List<OAuth2Jwk> findAllActiveJwks();
    
    /**
     * 停用过期的JWK
     */
    @Update("UPDATE oauth2_jwk SET is_active = 0 WHERE expires_at <= #{expireTime}")
    int deactivateExpiredJwks(LocalDateTime expireTime);
}

