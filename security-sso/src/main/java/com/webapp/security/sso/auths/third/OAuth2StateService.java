package com.webapp.security.sso.auths.third;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * GitHub OAuth2 State管理服务
 * 用于生成、存储和验证OAuth2授权过程中的state参数
 * 防止CSRF攻击
 */
@Service
public class OAuth2StateService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 生成并存储state
     * 
     * @return 生成的state值
     */
    public String generateAndSaveState(String prefix, int expire) {
        String state = UUID.randomUUID().toString();
        String key = prefix + state;
        redisTemplate.opsForValue().set(key, "1", expire, TimeUnit.SECONDS);
        return state;
    }

    public void saveState(String state, String prefix, int expire) {
        redisTemplate.opsForValue().set(prefix + state, state, expire, TimeUnit.SECONDS);
    }

    /**
     * 验证state是否有效
     * 
     * @param state 待验证的state值
     * @return 如果state有效返回true，否则返回false
     */
    public boolean validateState(String state, String prefix) {
        if (state == null || state.trim().isEmpty()) {
            return false;
        }

        String key = prefix + state;
        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            // 验证成功后删除state，确保一次性使用
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }
}