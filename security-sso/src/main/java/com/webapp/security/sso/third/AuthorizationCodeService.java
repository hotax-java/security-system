package com.webapp.security.sso.third;

import com.alibaba.fastjson.JSON;
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
    // 注释掉token_code相关常量 - 简化第三方认证流程，直接使用标准OAuth2流程
    // private static final String TOKEN_PREFIX = "oauth2:token:code:";
    private static final int CODE_LENGTH = 32;
    private static final int DEFAULT_EXPIRE_MINUTES = 5;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
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
        
        redisTemplate.opsForValue().set(key, JSON.toJSONString(data), DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        logger.info("生成绑定code: {}, platform: {}, 有效期: {}分钟", code, platform, DEFAULT_EXPIRE_MINUTES);
        return code;
    }
    

    
    /*
     * 注释掉token_code生成方法 - 简化第三方认证流程，直接使用标准OAuth2流程
     * 原方法用于缓存userId供前端兑换token，现在改为直接重定向到OAuth2授权端点
     */
    /*
    public String generateTokenCode(Long userId) {
        String code = generateRandomCode();
        String key = TOKEN_PREFIX + code;
        
        redisTemplate.opsForValue().set(key, userId, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        logger.info("生成token code: {}, userId: {}, 有效期: {}分钟", code, userId, DEFAULT_EXPIRE_MINUTES);
        return code;
    }
    */
    
    /**
     * 验证并获取绑定code数据（一次性使用）
     * 
     * @param code 要验证的code
     * @return 绑定数据，如果code无效或已过期则返回null
     */
    public BindCodeData validateAndConsumeBindCode(String code) {
        String key = CODE_PREFIX + "bind:" + code;
        
        try {
            Object value = redisTemplate.opsForValue().get(key);
            BindCodeData data = value != null ? JSON.parseObject(value.toString(), BindCodeData.class) : null;
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
    

    
    /*
     * 注释掉token_code验证方法 - 简化第三方认证流程，直接使用标准OAuth2流程
     * 原方法用于验证并消费token_code返回userId，现在改为直接使用OAuth2标准流程
     */
    /*
    public Long validateAndConsumeTokenCode(String code) {
        String key = TOKEN_PREFIX + code;
        
        try {
            Object userIdObj = redisTemplate.opsForValue().get(key);
            if (userIdObj != null) {
                // 立即删除code（一次性使用）
                redisTemplate.delete(key);
                Long userId = (Long) userIdObj;
                logger.info("验证并消费token code成功: {}, userId: {}", code, userId);
                return userId;
            } else {
                logger.warn("token code无效或已过期: {}", code);
                return null;
            }
        } catch (Exception e) {
            logger.error("验证token code时发生错误: {}", code, e);
            return null;
        }
    }
    */
    
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