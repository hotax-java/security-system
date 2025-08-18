package com.webapp.security.core.service;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.entity.SysWechatUser;
import com.webapp.security.core.mapper.SysUserMapper;
import com.webapp.security.core.mapper.SysWechatUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 微信用户关联服务类
 */
@Service
@RequiredArgsConstructor
public class SysWechatUserService {

    private final SysWechatUserMapper wechatUserMapper;
    private final SysUserMapper userMapper;

    /**
     * 根据ID查询
     */
    public Optional<SysWechatUser> findById(Long id) {
        return Optional.ofNullable(wechatUserMapper.selectById(id));
    }

    /**
     * 根据OpenID查询
     */
    public Optional<SysWechatUser> findByOpenid(String openid) {
        return Optional.ofNullable(wechatUserMapper.selectByOpenid(openid));
    }

    /**
     * 根据UnionID查询
     */
    public Optional<SysWechatUser> findByUnionid(String unionid) {
        return Optional.ofNullable(wechatUserMapper.selectByUnionid(unionid));
    }

    /**
     * 根据用户ID查询
     */
    public Optional<SysWechatUser> findByUserId(Long userId) {
        return Optional.ofNullable(wechatUserMapper.selectByUserId(userId));
    }

    /**
     * 创建微信用户关联
     */
    @Transactional
    public SysWechatUser create(SysWechatUser wechatUser) {
        wechatUserMapper.insert(wechatUser);
        return wechatUser;
    }

    /**
     * 更新微信用户关联
     */
    @Transactional
    public SysWechatUser update(SysWechatUser wechatUser) {
        wechatUserMapper.update(wechatUser);
        return wechatUser;
    }

    /**
     * 删除微信用户关联
     */
    @Transactional
    public void deleteById(Long id) {
        wechatUserMapper.deleteById(id);
    }

    /**
     * 根据OpenID删除微信用户关联
     */
    @Transactional
    public void deleteByOpenid(String openid) {
        wechatUserMapper.deleteByOpenid(openid);
    }

    /**
     * 根据用户ID删除微信用户关联
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        wechatUserMapper.deleteByUserId(userId);
    }

    /**
     * 处理微信OAuth2用户
     * 如果已关联系统用户，则返回关联的用户ID
     * 如果未关联，则返回空
     */
    public Optional<Long> processWechatUser(String openid, String unionid, String nickname, String headimgurl,
            String accessToken, String refreshToken) {
        // 查找是否已存在关联
        Optional<SysWechatUser> existingUser = findByOpenid(openid);

        if (existingUser.isPresent()) {
            // 已存在关联，更新信息
            SysWechatUser wechatUser = existingUser.get();
            wechatUser.setNickname(nickname);
            wechatUser.setHeadimgurl(headimgurl);
            wechatUser.setAccessToken(accessToken);
            wechatUser.setRefreshToken(refreshToken);
            if (unionid != null && !unionid.isEmpty()) {
                wechatUser.setUnionid(unionid);
            }
            update(wechatUser);
            return Optional.of(wechatUser.getUserId());
        }

        // 未找到关联，返回空
        return Optional.empty();
    }

    /**
     * 绑定微信用户到系统用户
     */
    @Transactional
    public SysWechatUser bindWechatToUser(String openid, String unionid, String nickname, String headimgurl,
            String accessToken, String refreshToken, Long userId) {
        // 检查用户是否存在
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        // 创建新的微信用户关联
        SysWechatUser wechatUser = new SysWechatUser();
        wechatUser.setOpenid(openid);
        wechatUser.setUnionid(unionid);
        wechatUser.setUserId(userId);
        wechatUser.setNickname(nickname);
        wechatUser.setHeadimgurl(headimgurl);
        wechatUser.setAccessToken(accessToken);
        wechatUser.setRefreshToken(refreshToken);

        create(wechatUser);
        return wechatUser;
    }
}