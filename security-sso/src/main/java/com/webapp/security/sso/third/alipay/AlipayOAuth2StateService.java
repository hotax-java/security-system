package com.webapp.security.sso.third.alipay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 支付宝OAuth2状态服务
 * 用于生成和验证state参数，防止CSRF攻击
 */
@Service
public class AlipayOAuth2StateService {
    private static final Logger logger = LoggerFactory.getLogger(AlipayOAuth2StateService.class);
    private static final String ALIPAY_STATE_PREFIX = "alipay:oauth2:state:";
    private static final long STATE_EXPIRE_SECONDS = 300; // 5分钟过期

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 生成并保存state
     */
    public String generateAndSaveState() {
        String state = UUID.randomUUID().toString();
        String key = ALIPAY_STATE_PREFIX + state;

        // 将state保存到Redis，设置过期时间
        redisTemplate.opsForValue().set(key, state, STATE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return state;
    }

    /**
     * 验证state
     */
    public boolean validateState(String state) {
        if (state == null || state.isEmpty()) {
            logger.warn("State参数为空");
            return false;
        }

        String key = ALIPAY_STATE_PREFIX + state;
        String savedState = redisTemplate.opsForValue().get(key);

        if (savedState == null) {
            logger.warn("State参数无效或已过期: {}", state);
            return false;
        }

        // 验证成功后删除state，确保一次性使用
        redisTemplate.delete(key);

        return true;
    }
}