package com.webapp.security.sso.third;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis Code管理服务
 * 用于生成、验证和删除一次性code的通用服务
 */
@Service
public class AuthorizationCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationCodeService.class);
    
    private static final String CODE_PREFIX = "oauth2:code:";
    private static final String TOKEN_PREFIX = "oauth2:token:";
    private static final int CODE_LENGTH = 32;
    private static final int DEFAULT_EXPIRE_MINUTES = 5;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 生成并存储一次性code用于绑定/创建用户验证
     * 
     * @param encryptedId 加密的第三方用户ID
     * @param platform 第三方平台（github、wechat、alipay）
     * @return 生成的code
     */
    public String generateBindCode(String encryptedId, String platform) {
        String code = generateRandomCode();
        String key = CODE_PREFIX + "bind:" + code;
        
        // 存储绑定信息
        BindCodeData data = new BindCodeData();
        data.setEncryptedId(encryptedId);
        data.setPlatform(platform);
        data.setType("bind");
        
        redisTemplate.opsForValue().set(key, data, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        logger.info("生成绑定code: {}, platform: {}, 有效期: {}分钟", code, platform, DEFAULT_EXPIRE_MINUTES);
        return code;
    }
    

    
    /**
     * 生成并存储userId code用于前端兑换token
     * 
     * @param userId 用户ID
     * @return 生成的code
     */
    public String generateUserCode(Long userId) {
        String code = generateRandomCode();
        String key = CODE_PREFIX + "user:" + code;
        
        redisTemplate.opsForValue().set(key, userId, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        logger.info("生成用户code: {}, userId: {}, 有效期: {}分钟", code, userId, DEFAULT_EXPIRE_MINUTES);
        return code;
    }
    
    /**
     * 验证并获取绑定code数据（一次性使用）
     * 
     * @param code 要验证的code
     * @return 绑定数据，如果code无效或已过期则返回null
     */
    public BindCodeData validateAndConsumeBindCode(String code) {
        String key = CODE_PREFIX + "bind:" + code;
        
        try {
            BindCodeData data = (BindCodeData) redisTemplate.opsForValue().get(key);
            if (data != null) {
                // 立即删除code（一次性使用）
                redisTemplate.delete(key);
                logger.info("验证并消费绑定code成功: {}, platform: {}", code, data.getPlatform());
                return data;
            } else {
                logger.warn("绑定code无效或已过期: {}", code);
                return null;
            }
        } catch (Exception e) {
            logger.error("验证绑定code时发生错误: {}", code, e);
            return null;
        }
    }
    

    
    /**
     * 验证并获取userId（一次性使用）
     * 
     * @param code 要验证的code
     * @return userId，如果code无效或已过期则返回null
     */
    public Long validateAndConsumeUserCode(String code) {
        String key = CODE_PREFIX + "user:" + code;
        
        try {
            Object userIdObj = redisTemplate.opsForValue().get(key);
            if (userIdObj != null) {
                // 立即删除code（一次性使用）
                redisTemplate.delete(key);
                Long userId = (Long) userIdObj;
                logger.info("验证并消费用户code成功: {}, userId: {}", code, userId);
                return userId;
            } else {
                logger.warn("用户code无效或已过期: {}", code);
                return null;
            }
        } catch (Exception e) {
            logger.error("验证用户code时发生错误: {}", code, e);
            return null;
        }
    }
    
    /**
     * 生成随机code
     * 
     * @return 32位随机字符串
     */
    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * 绑定code数据类
     */
    public static class BindCodeData {
        private String encryptedId;
        private String platform;
        private String type;
        
        public String getEncryptedId() {
            return encryptedId;
        }
        
        public void setEncryptedId(String encryptedId) {
            this.encryptedId = encryptedId;
        }
        
        public String getPlatform() {
            return platform;
        }
        
        public void setPlatform(String platform) {
            this.platform = platform;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
}