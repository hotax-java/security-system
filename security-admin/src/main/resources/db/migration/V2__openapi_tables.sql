-- OPENAPI 相关表（在 admin 库下创建）

-- sys_client_credential
CREATE TABLE IF NOT EXISTS sys_client_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    app_id VARCHAR(100) NOT NULL UNIQUE COMMENT '应用唯一标识（AppID）',
    app_secret VARCHAR(200) NOT NULL COMMENT '应用密钥（BCrypt 加密存储）',
    client_id VARCHAR(100) NOT NULL COMMENT '关联 oauth2_registered_client.client_id（固定为 openapi）',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    remark VARCHAR(500) COMMENT '备注信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AppID/AppSecret 凭证表';

-- sys_resource
CREATE TABLE IF NOT EXISTS sys_resource (
    resource_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    resource_code VARCHAR(100) NOT NULL UNIQUE COMMENT '资源编码（如user:query）',
    resource_name VARCHAR(100) NOT NULL COMMENT '资源名称（如用户查询接口）',
    resource_path VARCHAR(500) NOT NULL COMMENT '接口路径（如/api/v1/users/**）',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP方法（GET/POST/PUT/DELETE）',
    qps_limit INT DEFAULT NULL COMMENT '每秒请求上限（资源级，NULL 表示不限制）',
    burst_capacity INT DEFAULT NULL COMMENT '令牌桶突发容量（资源级，可选）',
    daily_quota INT DEFAULT NULL COMMENT '每日调用上限（资源级，可选）',
    concurrency_limit INT DEFAULT NULL COMMENT '并发限制（资源级，可选）',
    status TINYINT DEFAULT 1 COMMENT '资源状态（1-启用，0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_resource_path (resource_path, method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OPENAPI资源定义表';

-- sys_credential_resource_rel
CREATE TABLE IF NOT EXISTS sys_credential_resource_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    credential_id BIGINT NOT NULL COMMENT '关联 sys_client_credential.id',
    resource_id BIGINT NOT NULL COMMENT '关联 sys_resource.resource_id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(授权时间)',
    create_by VARCHAR(50) COMMENT '授权人(创建人)',
    UNIQUE KEY uk_cred_resource (credential_id, resource_id) COMMENT '避免重复授权',
    INDEX idx_cred (credential_id),
    INDEX idx_res (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App 凭证-API 资源授权关系表'; 