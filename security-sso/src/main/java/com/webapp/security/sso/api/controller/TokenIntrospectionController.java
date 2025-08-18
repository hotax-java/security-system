package com.webapp.security.sso.api.controller;

import com.webapp.security.core.model.OAuth2ErrorResponse;
import com.webapp.security.sso.api.service.TokenIntrospectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenIntrospection;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 令牌自省控制器
 * 处理令牌自省请求
 */
@RestController
public class TokenIntrospectionController {

    private static final Logger logger = LoggerFactory.getLogger(TokenIntrospectionController.class);

    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final TokenIntrospectionService tokenIntrospectionService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public TokenIntrospectionController(
            OAuth2AuthorizationService authorizationService,
            RegisteredClientRepository registeredClientRepository,
            TokenIntrospectionService tokenIntrospectionService,
            PasswordEncoder passwordEncoder) {
        this.authorizationService = authorizationService;
        this.registeredClientRepository = registeredClientRepository;
        this.tokenIntrospectionService = tokenIntrospectionService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 处理令牌自省请求
     * 这个端点将被资源服务器调用，用于验证令牌的有效性和获取权限信息
     *
     * @param token 令牌值
     * @return 令牌自省结果
     */
    @PostMapping("/v1/oauth2/introspect")
    public ResponseEntity<?> introspect(
            @RequestParam("token") String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("Received introspection request for token: {}...", token.substring(0, Math.min(token.length(), 8)));

        // 1. 验证客户端身份并获取RegisteredClient
        OAuth2ClientAuthenticationToken clientAuthentication;
        try {
            clientAuthentication = authenticateClient(authHeader);
        } catch (BadCredentialsException e) {
            logger.warn("Client authentication failed: {}", e.getMessage());
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.UNAUTHORIZED_CLIENT, e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
        // 设置认证上下文
        SecurityContextHolder.getContext().setAuthentication(clientAuthentication);

        // 查找并验证令牌
        OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);

        // 如果仍然找不到，令牌无效
        if (authorization == null) {
            Map<String, Object> inactiveResponse = new HashMap<>();
            inactiveResponse.put("active", false);
            inactiveResponse.put("error", "invalid_token");
            inactiveResponse.put("error_description", "Token is inactive or expired");
            return ResponseEntity.ok(inactiveResponse);
        }

        // 检查令牌是否过期
        OAuth2Authorization.Token<?> tokenMetadata = authorization.getToken(token);
        if (tokenMetadata == null || !tokenMetadata.isActive()) {
            Map<String, Object> inactiveResponse = new HashMap<>();
            inactiveResponse.put("active", false);
            inactiveResponse.put("error", "invalid_token");
            inactiveResponse.put("error_description", "Token is inactive or expired");
            return ResponseEntity.ok(inactiveResponse);
        }

        // 构建自省响应
        Map<String, Object> response = tokenIntrospectionService.introspect(authorization);

        return ResponseEntity.ok(response);
    }

    /**
     * 验证客户端身份并返回RegisteredClient
     *
     * @param authHeader Authorization头
     * @return 如果认证成功则返回RegisteredClient，否则返回null
     */
    private OAuth2ClientAuthenticationToken authenticateClient(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Basic ")) {
            logger.warn("Missing or invalid Authorization header format");
            throw new BadCredentialsException("Missing or invalid Authorization header format");
        }

        // 解析Basic认证头
        String base64Credentials = authHeader.substring("Basic ".length());
        byte[] credentialsBytes = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credentialsBytes);
        String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            logger.warn("Invalid Basic authentication format (missing colon)");
            throw new BadCredentialsException("Invalid Basic authentication format");
        }

        String clientId = values[0];
        String clientSecret = values[1];

        logger.debug("Attempting to authenticate client: {}", clientId);

        // 查找客户端
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            logger.warn("Client not found: {}", clientId);
            throw new BadCredentialsException("Client not found");
        }

        // 验证客户端密钥
        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            logger.warn("Invalid client secret for client: {}", clientId);
            throw new BadCredentialsException("Invalid client secret");
        }

        logger.info("Client authentication successful: {}", clientId);
        return new OAuth2ClientAuthenticationToken(
                client,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                client.getClientSecret());
    }
}