package com.webapp.security.sso.oauth2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 令牌黑名单服务
 * 用于管理已撤销的JWT令牌
 */
@Slf4j
@Service
public class TokenBlacklistService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 将令牌添加到黑名单
     * 
     * @param jti            JWT ID (JWT的唯一标识符)
     * @param expirationTime 令牌过期时间（秒）
     */
    @CacheEvict(value = "token-blacklist", key = "#jti")
    public void blacklistToken(String jti, long expirationTime) {
        String key = BLACKLIST_PREFIX + jti;
        Duration ttl = Duration.ofSeconds(expirationTime);

        redisTemplate.opsForValue().set(key, "revoked", ttl);
        log.info("令牌已加入黑名单: {}", jti);
    }

    /**
     * 检查令牌是否在黑名单中
     * 
     * @param jti JWT ID
     * @return true 如果在黑名单中，false 否则
     */
    @Cacheable(value = "token-blacklist", key = "#jti")
    public boolean isBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        Boolean hasKey = redisTemplate.hasKey(key);

        if (hasKey != null && hasKey) {
            log.debug("令牌在黑名单中: {}", jti);
            return true;
        }

        return false;
    }

    /**
     * 从黑名单中移除令牌（手动清理）
     * 
     * @param jti JWT ID
     */
    @CacheEvict(value = "token-blacklist", key = "#jti")
    public void removeFromBlacklist(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.delete(key);
        log.info("令牌已从黑名单移除: {}", jti);
    }

    /**
     * 获取黑名单中的令牌数量
     * 
     * @return 黑名单中的令牌数量
     */
    public long getBlacklistSize() {
        return redisTemplate.keys(BLACKLIST_PREFIX + "*").size();
    }

    /**
     * 清理过期的黑名单条目
     * Redis会自动清理过期的键，此方法用于手动清理
     */
    public void cleanupExpiredEntries() {
        // Redis会自动清理过期的键，这里只是记录日志
        log.info("黑名单清理完成，当前黑名单大小: {}", getBlacklistSize());
    }
}