package com.webapp.security.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PKCE参数管理服务
 * 负责生成、存储和获取PKCE相关参数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PkceService {

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${webapp.pkce.ttl:600}")
    private long ttl;

    @Value("${webapp.pkce.key-prefix:pkce:}")
    private String keyPrefix;

    /**
     * 生成PKCE参数组合
     * @return 包含state、code_verifier、code_challenge的Map
     */
    public Map<String, String> generatePkceParams() {
        try {
            // 生成state参数
            String state = generateState();
            
            // 生成code_verifier
            String codeVerifier = generateCodeVerifier();
            
            // 生成code_challenge
            String codeChallenge = generateCodeChallenge(codeVerifier);
            
            // 存储到Redis
            storePkceParams(state, codeVerifier, codeChallenge);
            
            // 返回给前端的参数
            Map<String, String> params = new HashMap<>();
            params.put("state", state);
            params.put("code_verifier", codeVerifier);
            params.put("code_challenge", codeChallenge);
            params.put("code_challenge_method", "S256");
            
            log.info("Generated PKCE params for state: {}", state);
            return params;
            
        } catch (Exception e) {
            log.error("Failed to generate PKCE params", e);
            throw new RuntimeException("生成PKCE参数失败", e);
        }
    }

    /**
     * 根据state获取code_verifier
     * @param state OAuth2 state参数
     * @return code_verifier，如果不存在或已过期返回null
     */
    public String getCodeVerifier(String state) {
        if (state == null || state.trim().isEmpty()) {
            log.warn("State parameter is empty");
            return null;
        }
        
        try {
            String key = keyPrefix + state;
            String codeVerifier = redisTemplate.opsForValue().get(key);
            
            if (codeVerifier != null) {
                log.info("Retrieved code_verifier for state: {}", state);
                // 获取后立即删除，防止重复使用
                redisTemplate.delete(key);
            } else {
                log.warn("No code_verifier found for state: {}", state);
            }
            
            return codeVerifier;
            
        } catch (Exception e) {
            log.error("Failed to get code_verifier for state: {}", state, e);
            return null;
        }
    }

    /**
     * 验证state参数是否存在
     * @param state OAuth2 state参数
     * @return 是否存在
     */
    public boolean validateState(String state) {
        if (state == null || state.trim().isEmpty()) {
            return false;
        }
        
        try {
            String key = keyPrefix + state;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to validate state: {}", state, e);
            return false;
        }
    }

    /**
     * 清理过期的PKCE参数（可选，Redis TTL会自动清理）
     * @param state OAuth2 state参数
     */
    public void cleanupPkceParams(String state) {
        if (state == null || state.trim().isEmpty()) {
            return;
        }
        
        try {
            String key = keyPrefix + state;
            redisTemplate.delete(key);
            log.info("Cleaned up PKCE params for state: {}", state);
        } catch (Exception e) {
            log.error("Failed to cleanup PKCE params for state: {}", state, e);
        }
    }

    /**
     * 生成随机state参数
     */
    private String generateState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成code_verifier
     * 根据RFC 7636规范，长度应在43-128个字符之间
     */
    private String generateCodeVerifier() {
        byte[] bytes = new byte[32]; // 32字节 = 43个Base64字符（去掉padding）
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 生成code_challenge
     * 使用SHA256哈希code_verifier，然后Base64 URL编码
     */
    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * 将PKCE参数存储到Redis
     */
    private void storePkceParams(String state, String codeVerifier, String codeChallenge) {
        try {
            String key = keyPrefix + state;
            // 只存储code_verifier，code_challenge可以重新计算
            redisTemplate.opsForValue().set(key, codeVerifier, Duration.ofSeconds(ttl));
            log.debug("Stored PKCE params in Redis with key: {} and TTL: {}s", key, ttl);
        } catch (Exception e) {
            log.error("Failed to store PKCE params in Redis", e);
            throw new RuntimeException("存储PKCE参数到Redis失败", e);
        }
    }
}