package com.webapp.security.sso.third.github;

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
public class GitHubOAuth2StateService {

    private static final String GITHUB_STATE_PREFIX = "github:oauth2:state:";
    private static final long STATE_EXPIRE_SECONDS = 600; // 10分钟过期

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 生成并存储state
     * 
     * @return 生成的state值
     */
    public String generateAndSaveState() {
        String state = UUID.randomUUID().toString();
        String key = GITHUB_STATE_PREFIX + state;
        redisTemplate.opsForValue().set(key, "1", STATE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return state;
    }

    /**
     * 验证state是否有效
     * 
     * @param state 待验证的state值
     * @return 如果state有效返回true，否则返回false
     */
    public boolean validateState(String state) {
        if (state == null || state.trim().isEmpty()) {
            return false;
        }

        String key = GITHUB_STATE_PREFIX + state;
        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            // 验证成功后删除state，确保一次性使用
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }
}