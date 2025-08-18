package com.webapp.security.core.service;

import com.webapp.security.core.entity.SysAlipayUser;

import java.util.Optional;

/**
 * 支付宝用户服务接口
 */
public interface SysAlipayUserService {
    /**
     * 根据支付宝用户ID查询
     */
    SysAlipayUser getByAlipayUserId(String alipayUserId);

    /**
     * 根据系统用户ID查询
     */
    SysAlipayUser getByUserId(Long userId);

    /**
     * 保存支付宝用户
     */
    boolean save(SysAlipayUser alipayUser);

    /**
     * 更新支付宝用户
     */
    boolean updateById(SysAlipayUser alipayUser);

    /**
     * 绑定系统用户
     */
    boolean bindUser(String alipayUserId, Long userId);

    /**
     * 更新令牌
     */
    boolean updateToken(Long id, String accessToken, String refreshToken);

    /**
     * 处理支付宝用户
     * 如果用户已存在，则更新信息并返回关联的系统用户ID
     * 如果用户不存在，则创建新用户并返回空
     */
    Optional<Long> processAlipayUser(String alipayUserId, String nickname, String avatar,
            String gender, String province, String city,
            String accessToken, String refreshToken);

    /**
     * 绑定支付宝用户到系统用户
     */
    SysAlipayUser bindAlipayToUser(String alipayUserId, String nickname, String avatar,
            String gender, String province, String city,
            String accessToken, String refreshToken, Long userId);
}