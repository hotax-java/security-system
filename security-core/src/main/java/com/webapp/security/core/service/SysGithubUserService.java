package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysGithubUser;

import java.util.Optional;

/**
 * GitHub用户服务接口
 */
public interface SysGithubUserService extends IService<SysGithubUser> {

    /**
     * 根据GitHub ID查询用户
     * 
     * @param githubId GitHub用户ID
     * @return GitHub用户信息
     */
    SysGithubUser getByGithubId(Long githubId);

    /**
     * 根据系统用户ID查询GitHub用户
     * 
     * @param userId 系统用户ID
     * @return GitHub用户信息
     */
    SysGithubUser getByUserId(Long userId);

    /**
     * 绑定GitHub用户到系统用户
     * 
     * @param githubId GitHub用户ID
     * @param userId   系统用户ID
     * @return 是否绑定成功
     */
    boolean bindUser(Long githubId, Long userId);

    /**
     * 处理GitHub用户（更新或创建）
     * @param githubId GitHub用户ID
     * @param login GitHub登录名
     * @param name 用户名
     * @param email 邮箱
     * @param avatarUrl 头像URL
     * @param bio 个人简介
     * @param location 位置
     * @param company 公司
     * @return 用户ID（如果已绑定）
     */
    Optional<Long> processGithubUser(Long githubId, String login, String name, String email, String avatarUrl, String bio, String location, String company);

    /**
     * 绑定GitHub用户到系统用户
     */
    SysGithubUser bindGithubToUser(Long githubId, String login, String name, String avatarUrl,
            String bio, String location, String company, Long userId);
}