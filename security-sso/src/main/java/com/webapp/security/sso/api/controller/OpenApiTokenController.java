package com.webapp.security.sso.api.controller;

import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.model.OAuth2ErrorResponse;
import com.webapp.security.core.service.SysClientCredentialService;
import com.webapp.security.sso.api.service.TokenIntrospectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI Token控制器
 * 处理客户端凭证授权流程，将appid/appsecret转换为OAuth2客户端凭证
 */
@RestController
@RequestMapping("/v1/oauth2")
@RequiredArgsConstructor
public class OpenApiTokenController {

    private static final Logger log = LoggerFactory.getLogger(OpenApiTokenController.class);
    private final SysClientCredentialService credentialService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<OAuth2Token> tokenGenerator;
    private final TokenIntrospectionService introspectionService;

    @Value("${oauth2.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    // 固定的OAuth2客户端配置
    private static final String OAUTH2_CLIENT_ID = "openapi";
    private static final String OAUTH2_CLIENT_SECRET = "IPSG-YbDDJ4C_tscD-OuYfrfSmVW8UKV";

    /**
     * OpenAPI Token获取端点
     * 兼容标准OAuth2客户端凭证流程
     *
     * @param request   HTTP请求
     * @param grantType 授权类型，必须为client_credentials
     * @return Token响应
     */
    @PostMapping("/token")
    public ResponseEntity<?> getToken(
            HttpServletRequest request,
            @RequestParam("grant_type") String grantType) {

        try {
            log.info("OpenAPI token request received, grant_type: {}", grantType);

            // 1. 验证授权类型
            if (!"client_credentials".equals(grantType)) {
                return createErrorResponse("unsupported_grant_type",
                        "grant_type " + grantType + " is not supported", 400);
            }

            // 2. 解析Authorization头
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return createErrorResponse("invalid_client",
                        "Missing or invalid Authorization header", 401);
            }

            // 3. 解码Basic认证
            String encodedCredentials = authHeader.substring(6); // 去掉"Basic "
            String decodedCredentials;
            try {
                decodedCredentials = new String(
                        Base64.getDecoder().decode(encodedCredentials),
                        StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return createErrorResponse("invalid_client",
                        "Invalid Base64 encoding in Authorization header", 401);
            }

            // 4. 分离appid和appsecret
            String[] credentials = decodedCredentials.split(":", 2);
            if (credentials.length != 2) {
                return createErrorResponse("invalid_client",
                        "Invalid credentials format", 401);
            }

            String appId = credentials[0];
            String appSecret = credentials[1];

            log.info("Parsed credentials - appId: {}", appId);

            // 5. 验证appid和appsecret
            SysClientCredential credential = credentialService.findByAppId(appId);
            if (credential == null) {
                log.warn("AppId not found: {}", appId);
                return createErrorResponse("invalid_client",
                        "Invalid client credentials", 401);
            }

            // 检查凭证状态
            if (credential.getStatus() == null || credential.getStatus() != 1) {
                log.warn("AppId disabled: {}", appId);
                return createErrorResponse("invalid_client",
                        "Client credentials disabled", 401);
            }

            // 验证密钥
            if (!passwordEncoder.matches(appSecret, credential.getAppSecret())) {
                log.warn("Invalid appSecret for appId: {}", appId);
                return createErrorResponse("invalid_client",
                        "Invalid client credentials", 401);
            }

            // 6. 验证clientId匹配
            if (!OAUTH2_CLIENT_ID.equals(credential.getClientId())) {
                log.warn("ClientId mismatch for appId: {}, expected: {}, actual: {}",
                        appId, OAUTH2_CLIENT_ID, credential.getClientId());
                return createErrorResponse("invalid_client",
                        "Client configuration error", 401);
            }

            log.info("Credentials validated successfully for appId: {}", appId);

            // 7. 直接使用Spring Security OAuth2的组件生成令牌
            log.info("Generating token for appId: {} using client credentials: {}", appId, OAUTH2_CLIENT_ID);

            // 7.1 获取注册的客户端
            RegisteredClient registeredClient = registeredClientRepository.findByClientId(OAUTH2_CLIENT_ID);
            if (registeredClient == null) {
                log.error("OAuth2 client not found: {}", OAUTH2_CLIENT_ID);
                return createErrorResponse("invalid_client", "OAuth2 client not found", 401);
            }

            // 7.2 验证客户端密钥
            if (!passwordEncoder.matches(OAUTH2_CLIENT_SECRET, registeredClient.getClientSecret())) {
                log.error("Invalid client secret for client: {}", OAUTH2_CLIENT_ID);
                return createErrorResponse("invalid_client", "Invalid client credentials", 401);
            }

            // 7.3 创建认证令牌
            OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
                    registeredClient, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, null);

            // 7.4 创建令牌上下文
            OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                    .registeredClient(registeredClient)
                    .principal(clientPrincipal)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .build();

            // 7.5 生成访问令牌
            OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);
            if (generatedToken == null) {
                log.error("Failed to generate token for client: {}", OAUTH2_CLIENT_ID);
                return createErrorResponse("server_error", "Failed to generate token", 500);
            }

            OAuth2AccessToken accessToken = (OAuth2AccessToken) generatedToken;

            List<String> permissions = introspectionService.getPermissionsFromDatabase(appId);

            // 7.6 创建OAuth2Authorization并包含appId
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                    .principalName(OAUTH2_CLIENT_ID)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .attribute("app_id", appId)
                    .attribute("permissions", String.join(" ", permissions));

            // 7.7 添加令牌到授权
            authorizationBuilder.token(accessToken, (metadata) -> {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, Collections.emptyMap());
            });

            // 7.8 构建并保存授权
            OAuth2Authorization authorization = authorizationBuilder.build();
            authorizationService.save(authorization);
            // 7.9 构造响应
            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken.getTokenValue());
            // 计算过期时间（秒）
            if (accessToken.getExpiresAt() != null) {
                long expiresIn = ChronoUnit.SECONDS.between(Instant.now(), accessToken.getExpiresAt());
                result.put("expires_in", expiresIn);
            } else {
                result.put("expires_in", registeredClient.getTokenSettings().getAccessTokenTimeToLive().getSeconds());
            }
            result.put("token_type", accessToken.getTokenType().getValue());

            log.info("Authorization saved with appId: {}", appId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing token request", e);
            return createErrorResponse("server_error",
                    "Internal server error", 500);
        }
    }

    /**
     * 创建错误响应
     */
    private ResponseEntity<OAuth2ErrorResponse> createErrorResponse(
            String error, String description, int status) {
        return OAuth2ErrorResponse.error(error, description, HttpStatus.valueOf(status));
    }
}