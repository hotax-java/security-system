package com.webapp.security.sso.oauth2.controller;

import com.webapp.security.core.config.ClientIdConfig;
import com.webapp.security.core.model.OAuth2ErrorResponse;
import com.webapp.security.sso.oauth2.context.ClientContext;
import com.webapp.security.sso.oauth2.model.LoginRequest;
import com.webapp.security.sso.oauth2.model.RefreshTokenRequest;
import com.webapp.security.sso.oauth2.model.LogoutRequest;
import com.webapp.security.sso.oauth2.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * OAuth2认证控制�?- 使用OAuth2TokenContext方式
 */
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    private final AuthenticationManager authenticationManager;
    //private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final OAuth2AuthorizationService authorizationService;

    // 添加OAuth2Utils依赖
    private final OAuth2Service oAuth2Service;

    // 添加ClientIdConfig依赖
    private final ClientIdConfig clientIdConfig;

    /**
     * 用户登录 - 使用OAuth2TokenContext方式
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 从ClientContext获取clientId，如果为空则使用默认的webapp客户端ID
            String clientId = ClientContext.getClientId();
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = clientIdConfig.getWebappClientId();
            }
            log.info("OAuth2 login attempt for user: {}, clientId: {}", loginRequest.getUsername(), clientId);

            // 1. 获取注册的客户端
            RegisteredClient registeredClient = oAuth2Service.getRegisteredClient(clientId);

            // 2. 进行身份验证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            // 4. 创建OAuth2授权
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                    .withRegisteredClient(registeredClient)
                    .principalName(authentication.getName())
                    .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                    .authorizedScopes(registeredClient.getScopes());

            // 5. 生成Access Token
            OAuth2AccessToken accessToken = oAuth2Service.generateAccessToken(authentication, registeredClient,
                    authorizationBuilder);

            // 6. 生成Refresh Token（可选）
            OAuth2RefreshToken refreshToken = oAuth2Service.generateRefreshToken(authentication, registeredClient,
                    authorizationBuilder);

            // 7. 保存授权信息
            OAuth2Authorization authorization = authorizationBuilder.build();
            authorizationService.save(authorization);

            // 8. 计算过期时间（秒）
            long expiresIn = 0;
            if (accessToken.getExpiresAt() != null) {
                expiresIn = Duration.between(Instant.now(), accessToken.getExpiresAt()).getSeconds();
            }

            // 9. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken.getTokenValue());
            response.put("token_type", accessToken.getTokenType().getValue());
            response.put("expires_in", expiresIn);
            response.put("scope", String.join(" ", accessToken.getScopes()));
            response.put("username", authentication.getName());
            response.put("client_id", clientId);

            if (refreshToken != null) {
                response.put("refresh_token", refreshToken.getTokenValue());
            }

            log.info("OAuth2 User login successful: {} for client: {}", loginRequest.getUsername(), clientId);
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            log.warn("OAuth2 Login failed for user: " + loginRequest.getUsername(), e);
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "用户名或密码错误", org.springframework.http.HttpStatus.UNAUTHORIZED);
        } catch (IllegalStateException e) {
            log.warn("OAuth2 Client error: " + e.getMessage());
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_CLIENT, e.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("OAuth2 Login error for user: " + loginRequest.getUsername(), e);
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.SERVER_ERROR, "服务器内部错误", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 用户登出 - 撤销授权记录
     * 授权记录是指OAuth2Authorization，包含用户的访问令牌、刷新令牌等信息
     * 这不是第三方授权登录记录，而是本授权服务器颁发的令牌授权记录
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest logoutRequest) {
        try {
            // 从ClientContext获取clientId，如果为空则使用默认的webapp客户端ID
            String clientId = ClientContext.getClientId();
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = clientIdConfig.getWebappClientId();
            }
            String accessToken = logoutRequest.getAccessToken();

            if (accessToken == null || accessToken.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "访问令牌不能为空");
                return ResponseEntity.status(400).body(errorResponse);
            }

            // 1. 根据访问令牌查找授权记录
            OAuth2Authorization authorization = authorizationService.findByToken(accessToken,
                    OAuth2TokenType.ACCESS_TOKEN);

            if (authorization != null) {
                // 2. 验证客户端ID（如果提供）
                if (clientId != null && !clientId.trim().isEmpty()) {
                    // 使用OAuth2Utils获取RegisteredClient
                    RegisteredClient registeredClient = oAuth2Service.getRegisteredClient(clientId);
                    if (registeredClient == null
                            || !registeredClient.getId().equals(authorization.getRegisteredClientId())) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "invalid_client");
                        errorResponse.put("error_description", "客户端ID不匹配");
                        return ResponseEntity.status(400).body(errorResponse);
                    }
                }

                // 3. 删除授权记录（撤销所有相关令牌）
                authorizationService.remove(authorization);

                log.info("OAuth2 Authorization revoked for user: {} client: {}",
                        authorization.getPrincipalName(),
                        authorization.getRegisteredClientId());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "登出成功，授权记录已撤销");
                response.put("revoked_tokens", "access_token, refresh_token");

                return ResponseEntity.ok(response);
            } else {
                // 令牌不存在或已过期
                Map<String, Object> response = new HashMap<>();
                response.put("message", "登出成功，令牌已失效");

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Logout error", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("error_description", "登出失败");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 刷新令牌 - 完整实现
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {
        try {
            String refreshTokenValue = refreshTokenRequest.getRefreshToken();
            // 从ClientContext获取clientId，如果为空则使用默认的webapp客户端ID
            String clientId = ClientContext.getClientId();
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = clientIdConfig.getWebappClientId();
            }

            // 1. 验证请求参数
            if (refreshTokenValue == null || refreshTokenValue.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "刷新令牌不能为空");
                return ResponseEntity.status(400).body(errorResponse);
            }

            if (clientId == null || clientId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_client");
                errorResponse.put("error_description", "客户端ID不能为空");
                return ResponseEntity.status(400).body(errorResponse);
            }

            // 2. 根据刷新令牌查找授权记录
            OAuth2Authorization authorization = authorizationService.findByToken(refreshTokenValue,
                    OAuth2TokenType.REFRESH_TOKEN);

            if (authorization == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_grant");
                errorResponse.put("error_description", "刷新令牌无效或已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // 3. 验证客户端ID
            // 直接通过clientId查找RegisteredClient，然后比较registeredClientId
            RegisteredClient registeredClientForValidation = oAuth2Service.getRegisteredClient(clientId);
            if (registeredClientForValidation == null
                    || !registeredClientForValidation.getId().equals(authorization.getRegisteredClientId())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_client");
                errorResponse.put("error_description", "客户端ID不匹配");
                return ResponseEntity.status(400).body(errorResponse);
            }

            // 4. 检查刷新令牌是否过期
            OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
            if (refreshToken == null || (refreshToken.getToken().getExpiresAt() != null &&
                    refreshToken.getToken().getExpiresAt().isBefore(Instant.now()))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_grant");
                errorResponse.put("error_description", "刷新令牌已过期");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // 5. 获取注册客户端和用户信息
            RegisteredClient registeredClient = oAuth2Service.getRegisteredClient(clientId);

            // 6. 重新构建认证信息 - 需要从UserDetailsService重新加载用户权限
            String username = authorization.getPrincipalName();

            // 从Spring上下文中获取UserDetailsService
            UserDetailsService userDetailsService = WebApplicationContextUtils
                    .getRequiredWebApplicationContext(request.getServletContext())
                    .getBean(UserDetailsService.class);

            // 加载完整的用户详情，包括权限
            UserDetails userDetails = userDetailsService
                    .loadUserByUsername(username);

            // 使用完整的用户权限创建新的认证对象
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities());

            // 7. 创建新的授权构建器
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.from(authorization);

            // 8. 生成新的访问令牌
            OAuth2AccessToken newAccessToken = oAuth2Service.generateAccessToken(authentication, registeredClient,
                    authorizationBuilder);

            // 9. 使用刷新令牌轮换机制
            // 每次刷新时生成新的refresh_token和新的授权记录
            log.info("使用token轮换机制，生成新的refresh_token，用户: {}, 客户端: {}",
                    authorization.getPrincipalName(), clientId);

            // 生成全新的刷新令牌，不再重用旧的
            OAuth2RefreshToken newRefreshToken = oAuth2Service.generateRefreshToken(authentication, registeredClient,
                    authorizationBuilder);

            // 仍然生成新的授权ID，避免覆盖原授权记录
            // 这样同时存在新旧两个授权记录，旧的会自然过期
            String newAuthorizationId = "refresh-" + UUID.randomUUID();
            authorizationBuilder.id(newAuthorizationId);

            // 10. 保存授权记录
            OAuth2Authorization newAuthorization = authorizationBuilder.build();
            authorizationService.save(newAuthorization);

            // 12. 计算过期时间
            long expiresIn = 0;
            if (newAccessToken.getExpiresAt() != null) {
                expiresIn = Duration.between(Instant.now(), newAccessToken.getExpiresAt()).getSeconds();
            }

            // 13. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", newAccessToken.getTokenValue());
            response.put("token_type", newAccessToken.getTokenType().getValue());
            response.put("expires_in", expiresIn);
            response.put("scope", String.join(" ", newAccessToken.getScopes()));
            response.put("refresh_token", newRefreshToken.getTokenValue());

            log.info("OAuth2 Token refreshed for user: {} client: {}",
                    authorization.getPrincipalName(), clientId);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.warn("OAuth2 Refresh token client error: " + e.getMessage());
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_CLIENT, e.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Refresh token error", e);
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.SERVER_ERROR, "刷新令牌失败", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
