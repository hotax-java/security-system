package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysGithubUser;
import com.webapp.security.core.mapper.SysGithubUserMapper;
import com.webapp.security.core.service.SysGithubUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * GitHub用户服务实现类
 */
@Service
public class SysGithubUserServiceImpl extends ServiceImpl<SysGithubUserMapper, SysGithubUser>
        implements SysGithubUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(SysGithubUserServiceImpl.class);

    @Override
    public SysGithubUser getByGithubId(Long githubId) {
        return getOne(new LambdaQueryWrapper<SysGithubUser>()
                .eq(SysGithubUser::getGithubId, githubId));
    }

    @Override
    public SysGithubUser getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<SysGithubUser>()
                .eq(SysGithubUser::getUserId, userId));
    }

    @Override
    @Transactional
    public boolean bindUser(Long githubId, Long userId) {
        SysGithubUser githubUser = getByGithubId(githubId);
        if (githubUser == null) {
            return false;
        }

        // 设置用户ID并更新
        githubUser.setUserId(userId);
        return updateById(githubUser);
    }

    @Override
    @Transactional
    public Optional<Long> processGithubUser(Long githubId, String login, String name,
            String email, String avatarUrl, String bio, String location, String company) {
        try {
            // 查询是否已存在GitHub用户
            SysGithubUser githubUser = getByGithubId(githubId);

            if (githubUser == null) {
                // 创建新的GitHub用户记录
                githubUser = new SysGithubUser();
                githubUser.setGithubId(githubId);
                githubUser.setLogin(login);
                githubUser.setName(name);
                githubUser.setEmail(email);
                githubUser.setAvatarUrl(avatarUrl);
                githubUser.setBio(bio);
                githubUser.setLocation(location);
                githubUser.setCompany(company);
                githubUser.setCreateTime(LocalDateTime.now());
                githubUser.setUpdateTime(LocalDateTime.now());

                save(githubUser);
                return Optional.empty();
            } else {
                // 更新GitHub用户信息
                githubUser.setLogin(login);
                githubUser.setName(name);
                githubUser.setEmail(email);
                githubUser.setAvatarUrl(avatarUrl);
                githubUser.setBio(bio);
                githubUser.setLocation(location);
                githubUser.setCompany(company);
                githubUser.setUpdateTime(LocalDateTime.now());

                updateById(githubUser);

                // 如果已关联系统用户，返回用户ID
                if (githubUser.getUserId() != null) {
                    return Optional.of(githubUser.getUserId());
                }

                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("处理GitHub用户信息失败", e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public SysGithubUser bindGithubToUser(Long githubId, String login, String name, String avatarUrl,
            String bio, String location, String company, Long userId) {
        // 查询是否已存在GitHub用户
        SysGithubUser githubUser = getByGithubId(githubId);

        if (githubUser == null) {
            // 创建新的GitHub用户记录
            githubUser = new SysGithubUser();
            githubUser.setGithubId(githubId);
            githubUser.setUserId(userId);
            githubUser.setLogin(login);
            githubUser.setName(name);
            githubUser.setAvatarUrl(avatarUrl);
            githubUser.setCreateTime(LocalDateTime.now());
            githubUser.setUpdateTime(LocalDateTime.now());

            save(githubUser);
        } else {
            // 更新GitHub用户信息
            githubUser.setUserId(userId);
            githubUser.setLogin(login);
            githubUser.setName(name);
            githubUser.setAvatarUrl(avatarUrl);
            githubUser.setUpdateTime(LocalDateTime.now());

            updateById(githubUser);
        }

        return githubUser;
    }
}