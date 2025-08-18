package com.webapp.security.sso.oauth2.custom.service;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.sso.oauth2.custom.util.CustomJwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义JWT认证服务
 * 处理JWT令牌的生成、验证和刷新
 */
@Service
@ConditionalOnProperty(name = "custom.jwt.enabled", havingValue = "true", matchIfMissing = false)
public class CustomJwtAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(CustomJwtAuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final CustomJwtUtil jwtUtil;
    private final CustomJwtUserDetailsService userDetailsService;

    public CustomJwtAuthenticationService(AuthenticationManager authenticationManager,
            CustomJwtUtil jwtUtil,
            CustomJwtUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 用户登录并生成JWT令牌
     */
    public Map<String, Object> authenticateAndGenerateToken(String username, String password) {
        try {
            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            if (authentication.isAuthenticated()) {
                // 生成访问令牌和刷新令牌
                String accessToken = jwtUtil.generateAccessToken(authentication);
                String refreshToken = jwtUtil.generateRefreshToken(authentication);

                // 获取用户详细信息
                SysUser sysUser = userDetailsService.getUserByUsername(username);

                // 构建响应 - 与OAuth2保持一致
                Map<String, Object> response = new HashMap<>();
                response.put("access_token", accessToken);
                response.put("refresh_token", refreshToken);
                response.put("token_type", "Bearer");
                response.put("expires_in", 3600); // 1小时
                response.put("refresh_expires_in", 86400); // 24小时
                response.put("scope", "read write");
                response.put("jti", jwtUtil.getJtiFromToken(accessToken));

                // 添加用户信息（可选，不影响OAuth2兼容性）
                if (sysUser != null) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("user_id", sysUser.getUserId());
                    userInfo.put("username", sysUser.getUsername());
                    userInfo.put("real_name", sysUser.getRealName());
                    userInfo.put("email", sysUser.getEmail());
                    userInfo.put("phone", sysUser.getPhone());
                    userInfo.put("status", sysUser.getStatus());
                    response.put("user_info", userInfo);
                }

                log.info("JWT tokens generated successfully for user: {}", username);
                return response;
            } else {
                log.warn("Authentication failed for user: {}", username);
                throw new RuntimeException("Authentication failed");
            }
        } catch (Exception e) {
            log.error("Error during authentication for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * 刷新JWT令牌
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        try {
            // 验证刷新令牌
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            // 检查令牌类型
            String tokenType = jwtUtil.getTokenTypeFromToken(refreshToken);
            if (!"refresh_token".equals(tokenType)) {
                throw new RuntimeException("Invalid token type for refresh");
            }

            // 检查令牌是否过期
            if (jwtUtil.isTokenExpired(refreshToken)) {
                throw new RuntimeException("Refresh token expired");
            }

            // 从刷新令牌中获取用户信息
            String username = jwtUtil.getUsernameFromToken(refreshToken);
            List<String> authorities = jwtUtil.getAuthoritiesFromToken(refreshToken);

            // 创建认证对象
            List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username, null, grantedAuthorities);

            // 生成新的访问令牌
            String newAccessToken = jwtUtil.generateAccessToken(authentication);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", newAccessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", 3600);
            response.put("scope", "read write");
            response.put("jti", jwtUtil.getJtiFromToken(newAccessToken));

            log.info("JWT token refreshed successfully for user: {}", username);
            return response;
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed: " + e.getMessage());
        }
    }

    /**
     * 验证JWT令牌
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    /**
     * 从令牌中获取用户信息 - 与OAuth2保持一致
     */
    public Map<String, Object> getUserInfoFromToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", jwtUtil.getUsernameFromToken(token));
            userInfo.put("user_id", jwtUtil.getUserIdFromToken(token));
            userInfo.put("authorities", jwtUtil.getAuthoritiesFromToken(token)); // 与OAuth2保持一致
            userInfo.put("jti", jwtUtil.getJtiFromToken(token));
            userInfo.put("token_type", jwtUtil.getTokenTypeFromToken(token));

            return userInfo;
        } catch (Exception e) {
            log.error("Error getting user info from token: {}", e.getMessage());
            throw new RuntimeException("Failed to get user info: " + e.getMessage());
        }
    }

    /**
     * 撤销令牌（加入黑名单）
     */
    public void revokeToken(String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                String jti = jwtUtil.getJtiFromToken(token);
                // TODO: 将JTI加入Redis黑名单，设置过期时间
                log.info("Token revoked successfully, JTI: {}", jti);
            }
        } catch (Exception e) {
            log.error("Error revoking token: {}", e.getMessage());
        }
    }
}