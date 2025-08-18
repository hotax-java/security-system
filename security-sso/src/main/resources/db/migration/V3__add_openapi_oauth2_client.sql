-- 添加OpenAPI客户端到OAuth2注册表
INSERT INTO oauth2_registered_client 
(
    id, 
    client_id, 
    client_id_issued_at, 
    client_secret, 
    client_secret_expires_at, 
    client_name, 
    client_authentication_methods, 
    authorization_grant_types, 
    redirect_uris, 
    scopes, 
    client_settings, 
    token_settings
) 
SELECT 
    'openapi', 
    'openapi',
    CURRENT_TIMESTAMP, 
    '$2a$10$P481Cd3PhuqME2s.Icnrre5vTsjbLkrxfqQzYFwbwjMFz3WyOJ1aq', -- BCrypt加密的密码，与sys_client_credential中保持一致
    NULL, -- 永不过期
    'OpenAPI客户端', 
    'client_secret_basic,client_secret_post', 
    'client_credentials,refresh_token', 
    '', -- 客户端模式不需要重定向URI
    'api:read,api:write', 
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":"RS256","settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"reference"},"settings.token.refresh-token-time-to-live":["java.time.Duration",86400.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
WHERE NOT EXISTS (
    SELECT 1 FROM oauth2_registered_client WHERE client_id = 'openapi'
); 