package com.webapp.security.sso.oauth2.expand;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webapp.security.sso.oauth2.entity.OAuth2RegisteredClient;
import com.webapp.security.sso.oauth2.mapper.OAuth2RegisteredClientMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2客户端注册服务
 */
@Service
@RequiredArgsConstructor
public class OAuth2RegisteredClientService implements RegisteredClientRepository {

    private static final Logger log = LoggerFactory.getLogger(OAuth2RegisteredClientService.class);

    private final OAuth2RegisteredClientMapper clientMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @CacheEvict(value = { "oauth2-clients-by-id", "oauth2-clients-by-client-id" }, allEntries = true)
    public void save(RegisteredClient registeredClient) {
        OAuth2RegisteredClient entity = toEntity(registeredClient);

        // 检查是否已存在
        LambdaQueryWrapper<OAuth2RegisteredClient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OAuth2RegisteredClient::getId, entity.getId());
        OAuth2RegisteredClient existing = clientMapper.selectOne(queryWrapper);

        if (existing != null) {
            // 更新
            clientMapper.updateById(entity);
            log.debug("Updated OAuth2 client: {}", entity.getClientId());
        } else {
            // 插入
            clientMapper.insert(entity);
            log.debug("Saved new OAuth2 client: {}", entity.getClientId());
        }
    }

    @Override
    @Cacheable(value = "oauth2-clients-by-id", key = "#id", unless = "#result == null")
    public RegisteredClient findById(String id) {
        OAuth2RegisteredClient entity = clientMapper.selectById(id);
        if (entity == null) {
            log.debug("OAuth2 client not found by id: {}", id);
            return null;
        }
        log.debug("Found OAuth2 client by id: {}", id);
        return toRegisteredClient(entity);
    }

    @Override
    @Cacheable(value = "oauth2-clients-by-client-id", key = "#clientId", unless = "#result == null")
    public RegisteredClient findByClientId(String clientId) {
        LambdaQueryWrapper<OAuth2RegisteredClient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OAuth2RegisteredClient::getClientId, clientId);
        OAuth2RegisteredClient entity = clientMapper.selectOne(queryWrapper);

        if (entity == null) {
            log.debug("OAuth2 client not found by clientId: {}", clientId);
            return null;
        }

        log.debug("Found OAuth2 client by clientId: {}", clientId);
        return toRegisteredClient(entity);
    }

    /**
     * 将实体转换为RegisteredClient
     */
    private RegisteredClient toRegisteredClient(OAuth2RegisteredClient entity) {
        try {
            RegisteredClient.Builder builder = RegisteredClient.withId(entity.getId())
                    .clientId(entity.getClientId())
                    .clientIdIssuedAt(entity.getClientIdIssuedAt() != null
                            ? entity.getClientIdIssuedAt().atZone(ZoneId.systemDefault()).toInstant()
                            : null)
                    .clientSecret(entity.getClientSecret())
                    .clientSecretExpiresAt(entity.getClientSecretExpiresAt() != null
                            ? entity.getClientSecretExpiresAt().atZone(ZoneId.systemDefault()).toInstant()
                            : null)
                    .clientName(entity.getClientName());

            // 客户端认证方法
            if (StringUtils.hasText(entity.getClientAuthenticationMethods())) {
                Arrays.stream(entity.getClientAuthenticationMethods().split(","))
                        .forEach(method -> builder
                                .clientAuthenticationMethod(new ClientAuthenticationMethod(method.trim())));
            }

            // 授权类型
            if (StringUtils.hasText(entity.getAuthorizationGrantTypes())) {
                Arrays.stream(entity.getAuthorizationGrantTypes().split(","))
                        .forEach(grantType -> builder
                                .authorizationGrantType(new AuthorizationGrantType(grantType.trim())));
            }

            // 重定向URI
            if (StringUtils.hasText(entity.getRedirectUris())) {
                Arrays.stream(entity.getRedirectUris().split(","))
                        .forEach(uri -> builder.redirectUri(uri.trim()));
            }

            // 作用域
            if (StringUtils.hasText(entity.getScopes())) {
                Set<String> scopes = Arrays.stream(entity.getScopes().split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet());
                builder.scopes(scopeSet -> scopeSet.addAll(scopes));
            }

            // 客户端设置
            if (StringUtils.hasText(entity.getClientSettings())) {
                Map<String, Object> clientSettingsMap = objectMapper.readValue(
                        entity.getClientSettings(), new TypeReference<Map<String, Object>>() {
                        });
                ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();

                if (clientSettingsMap.containsKey("settings.client.require-authorization-consent")) {
                    clientSettingsBuilder.requireAuthorizationConsent(
                            (Boolean) clientSettingsMap.get("settings.client.require-authorization-consent"));
                }
                if (clientSettingsMap.containsKey("settings.client.require-proof-key")) {
                    clientSettingsBuilder.requireProofKey(
                            (Boolean) clientSettingsMap.get("settings.client.require-proof-key"));
                }

                builder.clientSettings(clientSettingsBuilder.build());
            }

            // 令牌设置
            if (StringUtils.hasText(entity.getTokenSettings())) {
                Map<String, Object> tokenSettingsMap = objectMapper.readValue(
                        entity.getTokenSettings(), new TypeReference<Map<String, Object>>() {
                        });
                TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();

                if (tokenSettingsMap.containsKey("settings.token.access-token-time-to-live")) {
                    Object ttl = tokenSettingsMap.get("settings.token.access-token-time-to-live");
                    if (ttl instanceof Number) {
                        tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofSeconds(((Number) ttl).longValue()));
                    } else if (ttl instanceof List && ((List<?>) ttl).size() >= 2) {
                        // 处理ArrayList格式的数据
                        List<?> ttlList = (List<?>) ttl;
                        Object value = ttlList.get(1);
                        if (value instanceof Number) {
                            log.info("Setting access token TTL from List: {} seconds", ((Number) value).longValue());
                            tokenSettingsBuilder
                                    .accessTokenTimeToLive(Duration.ofSeconds(((Number) value).longValue()));
                        }
                    }
                }
                if (tokenSettingsMap.containsKey("settings.token.refresh-token-time-to-live")) {
                    Object ttl = tokenSettingsMap.get("settings.token.refresh-token-time-to-live");
                    if (ttl instanceof Number) {
                        tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(((Number) ttl).longValue()));
                    } else if (ttl instanceof List && ((List<?>) ttl).size() >= 2) {
                        // 处理ArrayList格式的数据
                        List<?> ttlList = (List<?>) ttl;
                        Object value = ttlList.get(1);
                        if (value instanceof Number) {
                            log.info("Setting refresh token TTL from List: {} seconds", ((Number) value).longValue());
                            tokenSettingsBuilder
                                    .refreshTokenTimeToLive(Duration.ofSeconds(((Number) value).longValue()));
                        }
                    }
                }
                if (tokenSettingsMap.containsKey("settings.token.reuse-refresh-tokens")) {
                    Object reuseRefreshTokens = tokenSettingsMap.get("settings.token.reuse-refresh-tokens");
                    if (reuseRefreshTokens instanceof Boolean) {
                        tokenSettingsBuilder.reuseRefreshTokens((Boolean) reuseRefreshTokens);
                    } else if (reuseRefreshTokens instanceof List && ((List<?>) reuseRefreshTokens).size() >= 2) {
                        // 处理ArrayList格式的数据
                        List<?> reuseRefreshTokensList = (List<?>) reuseRefreshTokens;
                        Object value = reuseRefreshTokensList.get(1);
                        if (value instanceof Boolean) {
                            log.info("Setting reuse refresh tokens from List: {}", value);
                            tokenSettingsBuilder.reuseRefreshTokens((Boolean) value);
                        }
                    }
                }

                // 处理令牌格式设置
                if (tokenSettingsMap.containsKey("settings.token.access-token-format")) {
                    Object formatObj = tokenSettingsMap.get("settings.token.access-token-format");
                    if (formatObj instanceof Map) {
                        Map<?, ?> formatMap = (Map<?, ?>) formatObj;
                        if (formatMap.containsKey("value")) {
                            String formatValue = (String) formatMap.get("value");
                            if ("reference".equals(formatValue)) {
                                log.info("Setting token format to REFERENCE for client: {}", entity.getClientId());
                                tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.REFERENCE);
                            } else if ("self-contained".equals(formatValue)) {
                                log.info("Setting token format to SELF_CONTAINED for client: {}", entity.getClientId());
                                tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
                            }
                        }
                    }
                }

                // 处理授权码生存时间
                if (tokenSettingsMap.containsKey("settings.token.authorization-code-time-to-live")) {
                    Object ttl = tokenSettingsMap.get("settings.token.authorization-code-time-to-live");
                    if (ttl instanceof Number) {
                        tokenSettingsBuilder
                                .authorizationCodeTimeToLive(Duration.ofSeconds(((Number) ttl).longValue()));
                    } else if (ttl instanceof List && ((List<?>) ttl).size() >= 2) {
                        // 处理ArrayList格式的数据
                        List<?> ttlList = (List<?>) ttl;
                        Object value = ttlList.get(1);
                        if (value instanceof Number) {
                            log.info("Setting authorization code TTL from List: {} seconds",
                                    ((Number) value).longValue());
                            tokenSettingsBuilder
                                    .authorizationCodeTimeToLive(Duration.ofSeconds(((Number) value).longValue()));
                        }
                    }
                }

                // 处理设备码生存时间
                if (tokenSettingsMap.containsKey("settings.token.device-code-time-to-live")) {
                    Object ttl = tokenSettingsMap.get("settings.token.device-code-time-to-live");
                    if (ttl instanceof Number) {
                        // 当前版本不支持deviceCodeTimeToLive方法
                        log.info("Device code TTL setting found but not supported in current version: {} seconds",
                                ((Number) ttl).longValue());
                    } else if (ttl instanceof List && ((List<?>) ttl).size() >= 2) {
                        // 处理ArrayList格式的数据
                        List<?> ttlList = (List<?>) ttl;
                        Object value = ttlList.get(1);
                        if (value instanceof Number) {
                            // 当前版本不支持deviceCodeTimeToLive方法
                            log.info("Device code TTL setting found but not supported in current version: {} seconds",
                                    ((Number) value).longValue());
                        }
                    }
                }

                // 处理ID令牌签名算法
                if (tokenSettingsMap.containsKey("settings.token.id-token-signature-algorithm")) {
                    Object sigAlg = tokenSettingsMap.get("settings.token.id-token-signature-algorithm");
                    if (sigAlg instanceof String) {
                        try {
                            SignatureAlgorithm algorithm = SignatureAlgorithm.from((String) sigAlg);
                            tokenSettingsBuilder.idTokenSignatureAlgorithm(algorithm);
                            log.info("Setting id-token-signature-algorithm to: {}", sigAlg);
                        } catch (Exception e) {
                            log.warn("Invalid signature algorithm: {}, using default RS256", sigAlg);
                            tokenSettingsBuilder.idTokenSignatureAlgorithm(SignatureAlgorithm.RS256);
                        }
                    } else {
                        tokenSettingsBuilder.idTokenSignatureAlgorithm(SignatureAlgorithm.RS256);
                    }
                } else {
                    tokenSettingsBuilder.idTokenSignatureAlgorithm(SignatureAlgorithm.RS256);
                }

                builder.tokenSettings(tokenSettingsBuilder.build());
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to convert entity to RegisteredClient", e);
            throw new RuntimeException("转换OAuth2客户端失败", e);
        }
    }

    /**
     * 将RegisteredClient转换为实体
     */
    private OAuth2RegisteredClient toEntity(RegisteredClient registeredClient) {
        try {
            OAuth2RegisteredClient entity = new OAuth2RegisteredClient();
            entity.setId(registeredClient.getId());
            entity.setClientId(registeredClient.getClientId());
            entity.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt() != null
                    ? LocalDateTime.ofInstant(registeredClient.getClientIdIssuedAt(), ZoneId.systemDefault())
                    : null);
            entity.setClientSecret(registeredClient.getClientSecret());
            entity.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt() != null
                    ? LocalDateTime.ofInstant(registeredClient.getClientSecretExpiresAt(), ZoneId.systemDefault())
                    : null);
            entity.setClientName(registeredClient.getClientName());

            // 客户端认证方法
            entity.setClientAuthenticationMethods(
                    registeredClient.getClientAuthenticationMethods().stream()
                            .map(ClientAuthenticationMethod::getValue)
                            .collect(Collectors.joining(",")));

            // 授权类型
            entity.setAuthorizationGrantTypes(
                    registeredClient.getAuthorizationGrantTypes().stream()
                            .map(AuthorizationGrantType::getValue)
                            .collect(Collectors.joining(",")));

            // 重定向URI
            entity.setRedirectUris(String.join(",", registeredClient.getRedirectUris()));

            // 作用域
            entity.setScopes(String.join(",", registeredClient.getScopes()));

            // 客户端设置
            entity.setClientSettings(
                    objectMapper.writeValueAsString(registeredClient.getClientSettings().getSettings()));

            // 令牌设置
            entity.setTokenSettings(objectMapper.writeValueAsString(registeredClient.getTokenSettings().getSettings()));

            return entity;

        } catch (Exception e) {
            log.error("Failed to convert RegisteredClient to entity", e);
            throw new RuntimeException("转换OAuth2客户端失败", e);
        }
    }
}
