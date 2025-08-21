package com.webapp.security.sso.auths.third;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * PKCE参数与state的存储服务
 */
@Service
public class PkceStateStore {

    private static final String REDIS_KEY_PREFIX = "pkce:state:";
    private static final long EXPIRY_SECONDS = 600; // 10分钟过期

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 保存PKCE参数与state的关联
     *
     * @param state               授权请求state
     * @param codeChallenge       PKCE码挑战
     * @param codeChallengeMethod PKCE码挑战方法(S256或plain)
     */
    public void savePkceParams(String state, String codeChallenge, String codeChallengeMethod) {
        String key = REDIS_KEY_PREFIX + state;
        Map<String, String> pkceData = new HashMap<>();
        pkceData.put("code_challenge", codeChallenge);
        pkceData.put("code_challenge_method", codeChallengeMethod);

        // 使用Hash结构存储PKCE参数
        redisTemplate.opsForHash().putAll(key, pkceData);
        redisTemplate.expire(key, EXPIRY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 获取指定state关联的PKCE参数
     *
     * @param state 授权请求state
     * @return 包含code_challenge和code_challenge_method的Map，如果不存在则返回null
     */
    public Map<String, String> getPkceParams(String state) {
        String key = REDIS_KEY_PREFIX + state;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    /**
     * 删除指定state的PKCE参数
     *
     * @param state 授权请求state
     */
    public void removePkceParams(String state) {
        String key = REDIS_KEY_PREFIX + state;
        redisTemplate.delete(key);
    }
}