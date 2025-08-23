-- auth-service 初始化脚本
-- 创建 OAuth2 相关表

-- OAuth2 客户端注册表
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id varchar(100) NOT NULL COMMENT '客户端唯一标识符',
    client_id varchar(100) NOT NULL COMMENT '客户端ID，用于客户端身份识别',
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '客户端ID签发时间',
    client_secret varchar(200) DEFAULT NULL COMMENT '客户端密钥，用于客户端身份验证',
    client_secret_expires_at timestamp DEFAULT NULL COMMENT '客户端密钥过期时间',
    client_name varchar(200) NOT NULL COMMENT '客户端名称，用于显示和识别',
    client_authentication_methods varchar(1000) NOT NULL COMMENT '客户端认证方法，如client_secret_basic,client_secret_post',
    authorization_grant_types varchar(1000) NOT NULL COMMENT '授权类型，如authorization_code,refresh_token,client_credentials',
    redirect_uris varchar(1000) DEFAULT NULL COMMENT '重定向URI列表，授权码模式下的回调地址',
    post_logout_redirect_uris varchar(1000) DEFAULT NULL COMMENT '注销后重定向URI列表',
    scopes varchar(1000) NOT NULL COMMENT '客户端可访问的权限范围，如read,write,openid',
    client_settings varchar(2000) NOT NULL COMMENT '客户端设置，JSON格式存储客户端配置信息',
    token_settings varchar(2000) NOT NULL COMMENT '令牌设置，JSON格式存储令牌相关配置',
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth2_registered_client_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2客户端注册表';

-- OAuth2 授权表（用于有状态实现）
CREATE TABLE oauth2_authorization (
  id varchar(100) NOT NULL,
  registered_client_id varchar(100) NOT NULL,
  principal_name varchar(200) NOT NULL,
  authorization_grant_type varchar(100) NOT NULL,
  authorized_scopes varchar(1000) DEFAULT NULL,
  attributes blob DEFAULT NULL,
  state varchar(500) DEFAULT NULL,
  authorization_code_value blob DEFAULT NULL,
  authorization_code_issued_at timestamp DEFAULT NULL,
  authorization_code_expires_at timestamp DEFAULT NULL,
  authorization_code_metadata blob DEFAULT NULL,
  access_token_value blob DEFAULT NULL,
  access_token_issued_at timestamp DEFAULT NULL,
  access_token_expires_at timestamp DEFAULT NULL,
  access_token_metadata blob DEFAULT NULL,
  access_token_type varchar(100) DEFAULT NULL,
  access_token_scopes varchar(1000) DEFAULT NULL,
  oidc_id_token_value blob DEFAULT NULL,
  oidc_id_token_issued_at timestamp DEFAULT NULL,
  oidc_id_token_expires_at timestamp DEFAULT NULL,
  oidc_id_token_metadata blob DEFAULT NULL,
  refresh_token_value blob DEFAULT NULL,
  refresh_token_issued_at timestamp DEFAULT NULL,
  refresh_token_expires_at timestamp DEFAULT NULL,
  refresh_token_metadata blob DEFAULT NULL,
  user_code_value blob DEFAULT NULL,
  user_code_issued_at timestamp DEFAULT NULL,
  user_code_expires_at timestamp DEFAULT NULL,
  user_code_metadata blob DEFAULT NULL,
  device_code_value blob DEFAULT NULL,
  device_code_issued_at timestamp DEFAULT NULL,
  device_code_expires_at timestamp DEFAULT NULL,
  device_code_metadata blob DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2授权表';

-- OAuth2 授权同意表
CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL COMMENT '注册客户端ID，关联oauth2_registered_client表',
    principal_name varchar(200) NOT NULL COMMENT '用户主体名称，通常是用户名',
    authorities varchar(1000) NOT NULL COMMENT '用户已同意的权限列表，逗号分隔',
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2授权同意表';

-- OAuth2 JWK 密钥表
CREATE TABLE IF NOT EXISTS oauth2_jwk (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
    key_id varchar(100) NOT NULL COMMENT '密钥唯一标识符，用于JWT头部的kid字段',
    key_type varchar(20) NOT NULL DEFAULT 'RSA' COMMENT '密钥类型，如RSA、EC等',
    algorithm varchar(20) NOT NULL DEFAULT 'RS256' COMMENT '签名算法，如RS256、ES256等',
    public_key text NOT NULL COMMENT '公钥内容，PEM格式',
    private_key text NOT NULL COMMENT '私钥内容，PEM格式',
    created_time timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '密钥创建时间',
    expires_at timestamp NOT NULL COMMENT '密钥过期时间',
    is_active tinyint(1) DEFAULT 1 COMMENT '是否激活：1-激活，0-停用',
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth2_jwk_key_id (key_id),
    KEY idx_oauth2_jwk_expires_at (expires_at),
    KEY idx_oauth2_jwk_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 JWK密钥表';

-- 插入默认的 OAuth2 客户端
INSERT IGNORE INTO oauth2_registered_client (
    id, client_id, client_name, client_secret,
    client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'webapp-client-id',
    'webapp-client',
    'WebApp Client',
    '{noop}webapp-secret',
    'client_secret_basic,client_secret_post',
    'authorization_code,refresh_token,client_credentials',
    'https://885ro126ov70.vicp.fun/login/oauth2/code/webapp-client,https://885ro126ov70.vicp.fun/authorized,https://885ro126ov70.vicp.fun/test/callback',
    'read,write,openid,profile',
    '{"@class":"java.util.LinkedHashMap","settings.client.require-authorization-consent":false,"settings.client.require-proof-key":false}',
    '{"@class":"java.util.LinkedHashMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000]}'
);