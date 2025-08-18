package com.webapp.security.sso.oauth2.controller;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.webapp.security.sso.oauth2.entity.OAuth2Jwk;
import com.webapp.security.sso.oauth2.service.JwkService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JWK Set端点控制�? */
@RestController
@RequiredArgsConstructor
public class JwkSetController {
    
    private static final Logger log = LoggerFactory.getLogger(JwkSetController.class);
    
    private final JwkService jwkService;
    
    /**
     * JWK Set端点
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwkSet() {
        try {
            log.debug("JWK Set endpoint accessed");
            
            // 获取所有有效的JWK
            List<OAuth2Jwk> activeJwks = jwkService.getAllActiveJwks();
            
            // 构建JWK Set
            if (activeJwks.isEmpty()) {
                return new JWKSet().toJSONObject();
            }
            
            // 转换为JWK列表
            List<JWK> jwks = new ArrayList<>();
            for (OAuth2Jwk jwk : activeJwks) {
                RSAKey rsaKey = jwkService.toRSAKey(jwk);
                // 只包含公钥信
                jwks.add(rsaKey.toPublicJWK());
            }
            
            JWKSet jwkSet = new JWKSet(jwks);
            
            log.debug("Returning JWK Set with {} keys", activeJwks.size());
            return jwkSet.toJSONObject();
            
        } catch (Exception e) {
            log.error("Error generating JWK Set", e);
            throw new RuntimeException("生成JWK Set失败", e);
        }
    }
}

