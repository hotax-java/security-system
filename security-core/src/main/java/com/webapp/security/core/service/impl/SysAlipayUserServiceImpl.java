package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysAlipayUser;
import com.webapp.security.core.mapper.SysAlipayUserMapper;
import com.webapp.security.core.service.SysAlipayUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 支付宝用户服务实现类
 */
@Service
public class SysAlipayUserServiceImpl extends ServiceImpl<SysAlipayUserMapper, SysAlipayUser>
        implements SysAlipayUserService {
    private static final Logger logger = LoggerFactory.getLogger(SysAlipayUserServiceImpl.class);

    @Override
    public SysAlipayUser getByAlipayUserId(String alipayUserId) {
        return baseMapper.selectByAlipayUserId(alipayUserId);
    }

    @Override
    public SysAlipayUser getByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public boolean save(SysAlipayUser alipayUser) {
        return super.save(alipayUser);
    }

    @Override
    public boolean updateById(SysAlipayUser alipayUser) {
        return super.updateById(alipayUser);
    }

    @Override
    @Transactional
    public boolean bindUser(String alipayUserId, Long userId) {
        SysAlipayUser alipayUser = getByAlipayUserId(alipayUserId);
        if (alipayUser == null) {
            logger.warn("支付宝用户不存在: {}", alipayUserId);
            return false;
        }

        return baseMapper.bindUser(alipayUser.getId(), userId) > 0;
    }

    @Override
    @Transactional
    public boolean updateToken(Long id, String accessToken, String refreshToken) {
        return baseMapper.updateToken(id, accessToken, refreshToken) > 0;
    }

    @Override
    @Transactional
    public Optional<Long> processAlipayUser(String alipayUserId, String nickname, String avatar,
            String gender, String province, String city,
            String accessToken, String refreshToken) {
        try {
            // 查询是否已存在支付宝用户
            SysAlipayUser alipayUser = getByAlipayUserId(alipayUserId);

            if (alipayUser == null) {
                // 创建新的支付宝用户记录
                alipayUser = new SysAlipayUser();
                alipayUser.setAlipayUserId(alipayUserId);
                alipayUser.setNickname(nickname);
                alipayUser.setAvatar(avatar);
                alipayUser.setGender(gender);
                alipayUser.setProvince(province);
                alipayUser.setCity(city);
                alipayUser.setAccessToken(accessToken);
                alipayUser.setRefreshToken(refreshToken);
                alipayUser.setCreateTime(LocalDateTime.now());
                alipayUser.setUpdateTime(LocalDateTime.now());

                save(alipayUser);
                return Optional.empty();
            } else {
                // 更新支付宝用户信息
                alipayUser.setNickname(nickname);
                alipayUser.setAvatar(avatar);
                alipayUser.setGender(gender);
                alipayUser.setProvince(province);
                alipayUser.setCity(city);
                alipayUser.setAccessToken(accessToken);
                alipayUser.setRefreshToken(refreshToken);
                alipayUser.setUpdateTime(LocalDateTime.now());

                updateById(alipayUser);

                // 如果已关联系统用户，返回用户ID
                if (alipayUser.getUserId() != null) {
                    return Optional.of(alipayUser.getUserId());
                }

                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("处理支付宝用户信息失败", e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public SysAlipayUser bindAlipayToUser(String alipayUserId, String nickname, String avatar,
            String gender, String province, String city,
            String accessToken, String refreshToken, Long userId) {
        // 查询是否已存在支付宝用户
        SysAlipayUser alipayUser = getByAlipayUserId(alipayUserId);

        if (alipayUser == null) {
            // 创建新的支付宝用户记录
            alipayUser = new SysAlipayUser();
            alipayUser.setAlipayUserId(alipayUserId);
            alipayUser.setUserId(userId);
            alipayUser.setNickname(nickname);
            alipayUser.setAvatar(avatar);
            alipayUser.setGender(gender);
            alipayUser.setProvince(province);
            alipayUser.setCity(city);
            alipayUser.setAccessToken(accessToken);
            alipayUser.setRefreshToken(refreshToken);
            alipayUser.setCreateTime(LocalDateTime.now());
            alipayUser.setUpdateTime(LocalDateTime.now());

            save(alipayUser);
        } else {
            // 更新支付宝用户信息
            alipayUser.setUserId(userId);
            alipayUser.setNickname(nickname);
            alipayUser.setAvatar(avatar);
            alipayUser.setGender(gender);
            alipayUser.setProvince(province);
            alipayUser.setCity(city);
            alipayUser.setAccessToken(accessToken);
            alipayUser.setRefreshToken(refreshToken);
            alipayUser.setUpdateTime(LocalDateTime.now());

            updateById(alipayUser);
        }

        return alipayUser;
    }
}