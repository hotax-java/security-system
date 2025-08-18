package com.webapp.security.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信用户关联实体类
 * 用于存储微信用户与系统用户的关联关系
 */
@Data
public class SysWechatUser {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 微信OpenID
     */
    private String openid;

    /**
     * 微信UnionID（如果有）
     */
    private String unionid;

    /**
     * 关联的系统用户ID
     */
    private Long userId;

    /**
     * 微信昵称
     */
    private String nickname;

    /**
     * 微信头像URL
     */
    private String headimgurl;

    /**
     * 微信访问令牌
     */
    private String accessToken;

    /**
     * 微信刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}