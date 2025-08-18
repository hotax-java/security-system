package com.webapp.security.sso.oauth2.custom.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.sso.oauth2.custom.service.CustomJwtUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义JWT工具类
 * 提供JWT的创建、验证、解析等功能
 */
@Component
@ConditionalOnProperty(name = "custom.jwt.enabled", havingValue = "true", matchIfMissing = false)
public class CustomJwtUtil {

    private static final Logger log = LoggerFactory.getLogger(CustomJwtUtil.class);

    @Value("${jwt.secret:your-secret-key-here-must-be-at-least-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:86400}")
    private long refreshExpiration;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomJwtUserDetailsService userDetailsService;

    public CustomJwtUtil(CustomJwtUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtExpiration, "access_token");
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, refreshExpiration, "refresh_token");
    }

    /**
     * 生成令牌
     */
    private String generateToken(Authentication authentication, long expiration, String tokenType) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(expiration, ChronoUnit.SECONDS);

        // 获取用户权限 - 与OAuth2保持一致
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> authoritiesList = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("Custom JWT authorities: {}", authoritiesList);

        // 获取用户详细信息
        SysUser sysUser = userDetailsService.getUserByUsername(authentication.getName());

        // 构建JWT声明 - 与OAuth2保持一致
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", authentication.getName());
        claims.put("authorities", authoritiesList); // 与OAuth2保持一致，权限信息放在authorities中
        claims.put("token_type", tokenType);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", now.getEpochSecond());
        claims.put("nbf", now.getEpochSecond());

        // 添加用户详细信息（可选，不影响OAuth2兼容性）
        if (sysUser != null) {
            claims.put("user_id", sysUser.getUserId());
            claims.put("real_name", sysUser.getRealName());
            claims.put("email", sysUser.getEmail());
            claims.put("phone", sysUser.getPhone());
            claims.put("status", sysUser.getStatus());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .setIssuer("security-sso")
                .setAudience("security-admin")
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object userId = claims.get("user_id");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    /**
     * 从令牌中获取权限 - 与OAuth2保持一致
     */
    public List<String> getAuthoritiesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object authoritiesObj = claims.get("authorities");

        if (authoritiesObj instanceof List) {
            return (List<String>) authoritiesObj;
        }
        return new ArrayList<>();
    }

    /**
     * 从令牌中获取令牌类型
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("token_type", String.class);
    }

    /**
     * 从令牌中获取JTI
     */
    public String getJtiFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("jti", String.class);
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从令牌中获取声明
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 解析令牌（不验证签名，仅用于调试）
     */
    public Claims parseTokenWithoutValidation(String token) {
        try {
            // 分割JWT令牌
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }

            // 解码payload部分
            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            // 解析为Claims对象
            return objectMapper.readValue(decodedPayload, Claims.class);
        } catch (Exception e) {
            log.error("Error parsing token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }
}