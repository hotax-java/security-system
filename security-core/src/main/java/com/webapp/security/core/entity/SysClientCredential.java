package com.webapp.security.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 客户端凭证实体类
 * 用于管理API调用者的认证信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_client_credential")
public class SysClientCredential {

    /**
     * 凭证主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用唯一标识（AppID）
     * 用于API调用时进行身份识别
     */
    @TableField("app_id")
    private String appId;

    /**
     * 应用密钥（AppSecret）
     * 经过BCrypt加密存储的密钥
     */
    @TableField("app_secret")
    private String appSecret;

    /**
     * 明文密钥（不持久化）
     * 仅用于创建凭证时返回给客户端，不会存储到数据库
     */
    @TableField(exist = false)
    private String plainSecret;

    /**
     * 关联OAuth2注册客户端的ID
     * 固定为"openapi"
     */
    @TableField("client_id")
    private String clientId;

    /**
     * 凭证状态
     * 1-启用，0-禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注信息
     * 用于描述凭证的用途、归属等
     */
    @TableField("remark")
    private String remark;

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

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}