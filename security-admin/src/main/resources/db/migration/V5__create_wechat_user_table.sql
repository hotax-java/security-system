-- 创建微信用户关联表
CREATE TABLE IF NOT EXISTS sys_wechat_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    openid VARCHAR(64) NOT NULL COMMENT '微信OpenID',
    unionid VARCHAR(64) DEFAULT NULL COMMENT '微信UnionID',
    user_id BIGINT NOT NULL COMMENT '关联的系统用户ID',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '微信昵称',
    headimgurl VARCHAR(255) DEFAULT NULL COMMENT '微信头像URL',
    access_token VARCHAR(255) DEFAULT NULL COMMENT '微信访问令牌',
    refresh_token VARCHAR(255) DEFAULT NULL COMMENT '微信刷新令牌',
    expires_at DATETIME DEFAULT NULL COMMENT '令牌过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_openid (openid),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_unionid (unionid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户关联表';