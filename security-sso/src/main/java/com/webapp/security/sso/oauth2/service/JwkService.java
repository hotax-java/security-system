package com.webapp.security.sso.oauth2.service;

import com.nimbusds.jose.jwk.RSAKey;
import com.webapp.security.sso.oauth2.entity.OAuth2Jwk;
import com.webapp.security.sso.oauth2.mapper.OAuth2JwkMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JWK密钥服务
 */
@Service
@Lazy
@RequiredArgsConstructor
public class JwkService {
    
    private static final Logger log = LoggerFactory.getLogger(JwkService.class);
    
    private final OAuth2JwkMapper jwkMapper;
    private final ReentrantLock keyGenerationLock = new ReentrantLock();
    
    /**
     * 获取当前有效的JWK，如果不存在或过期则生成新的
     */
    public OAuth2Jwk getCurrentJwk() {
        // 先尝试获取有效的JWK
        OAuth2Jwk activeJwk = jwkMapper.findActiveJwk();
        
        if (activeJwk != null) {
            log.debug("Found active JWK with keyId: {}", activeJwk.getKeyId());
            return activeJwk;
        }
        
        // 如果没有有效的JWK，则生成新的（使用锁防止并发生成）
        return generateNewJwkWithLock();
    }
    
    /**
     * 获取所有有效的JWK（用于JWK Set端点）
     */
    public List<OAuth2Jwk> getAllActiveJwks() {
        return jwkMapper.findAllActiveJwks();
    }
    
    /**
     * 使用锁生成新的JWK，防止并发问题
     */
    private OAuth2Jwk generateNewJwkWithLock() {
        keyGenerationLock.lock();
        try {
            // 再次检查是否有其他线程已经生成了JWK
            OAuth2Jwk activeJwk = jwkMapper.findActiveJwk();
            if (activeJwk != null) {
                log.debug("Another thread already generated JWK with keyId: {}", activeJwk.getKeyId());
                return activeJwk;
            }
            
            // 生成新的JWK
            return generateNewJwk();
        } finally {
            keyGenerationLock.unlock();
        }
    }
    
    /**
     * 生成新的JWK
     */
    @Transactional
    public OAuth2Jwk generateNewJwk() {
        try {
            log.info("Generating new JWK...");
            
            // 停用过期的JWK
            int deactivatedCount = jwkMapper.deactivateExpiredJwks(LocalDateTime.now());
            if (deactivatedCount > 0) {
                log.info("Deactivated {} expired JWKs", deactivatedCount);
            }
            
            // 生成RSA密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            
            // 创建JWK实体
            OAuth2Jwk jwk = new OAuth2Jwk();
            jwk.setKeyId(UUID.randomUUID().toString());
            jwk.setKeyType("RSA");
            jwk.setAlgorithm("RS256");
            jwk.setPublicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
            jwk.setPrivateKey(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
            jwk.setCreatedTime(LocalDateTime.now());
            jwk.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30天过期
            jwk.setIsActive(true);
            
            // 保存到数据库
            jwkMapper.insert(jwk);
            log.info("Generated new JWK with keyId: {}, expires at: {}", jwk.getKeyId(), jwk.getExpiresAt());
            
            return jwk;
            
        } catch (Exception e) {
            log.error("Failed to generate new JWK", e);
            throw new RuntimeException("生成JWK失败", e);
        }
    }
    
    /**
     * 将OAuth2Jwk转换为RSAKey
     */
    public RSAKey toRSAKey(OAuth2Jwk jwk) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(jwk.getPublicKey());
            byte[] privateKeyBytes = Base64.getDecoder().decode(jwk.getPrivateKey());
            
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new java.security.spec.X509EncodedKeySpec(publicKeyBytes));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                    new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes));
            
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(jwk.getKeyId())
                    .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to convert OAuth2Jwk to RSAKey", e);
            throw new RuntimeException("转换JWK失败", e);
        }
    }
}

