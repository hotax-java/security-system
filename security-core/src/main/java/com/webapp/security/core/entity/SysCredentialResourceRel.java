package com.webapp.security.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 客户端凭证与API资源关联实体类
 * 用于存储客户端凭证对API资源的授权关系
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_credential_resource_rel")
public class SysCredentialResourceRel {

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 客户端凭证ID
     * 关联sys_client_credential表的id字段
     */
    @TableField("credential_id")
    private Long credentialId;

    /**
     * 资源ID
     * 关联sys_resource表的resource_id字段
     */
    @TableField("resource_id")
    private Long resourceId;

    /**
     * 创建时间（授权时间）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 授权人
     * 记录执行授权操作的用户
     */
    @TableField("create_by")
    private String createBy;
}