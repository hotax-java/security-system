package com.webapp.security.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * GitHub用户实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_github_user")
public class SysGithubUser {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 系统用户ID（可为空，表示未绑定）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * GitHub用户ID
     */
    @TableField("github_id")
    private Long githubId;

    /**
     * GitHub登录名
     */
    @TableField("login")
    private String login;

    /**
     * GitHub用户名
     */
    @TableField("name")
    private String name;

    /**
     * GitHub邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 个人简介
     */
    @TableField("bio")
    private String bio;

    /**
     * 位置
     */
    @TableField("location")
    private String location;

    /**
     * 公司
     */
    @TableField("company")
    private String company;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}