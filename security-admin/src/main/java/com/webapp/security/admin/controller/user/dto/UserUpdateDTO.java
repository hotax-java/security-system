package com.webapp.security.admin.controller.user.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

/**
 * 用户更新请求DTO
 */
@Data
public class UserUpdateDTO {
    /**
     * 用户名
     */
    @Size(min = 4, max = 50, message = "用户名长度必须在4-50个字符之间")
    private String username;

    /**
     * 密码
     */
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态：0-禁用，1-启用
     */
    private Integer status;
}