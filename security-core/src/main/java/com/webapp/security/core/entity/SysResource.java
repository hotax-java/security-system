package com.webapp.security.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * API资源实体类
 * 用于管理系统中的API接口资源及其访问控制配置
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_resource")
public class SysResource {

    /**
     * 资源主键ID
     */
    @TableId(value = "resource_id", type = IdType.AUTO)
    private Long resourceId;

    /**
     * 资源编码
     * 如：user:query、order:create等，用于权限标识
     */
    @TableField("resource_code")
    private String resourceCode;

    /**
     * 资源名称
     * 如：用户查询、订单创建等
     */
    @TableField("resource_name")
    private String resourceName;

    /**
     * 资源路径
     * API的URL路径模式，如/api/v1/users/**
     */
    @TableField("resource_path")
    private String resourcePath;

    /**
     * HTTP请求方法
     * GET、POST、PUT、DELETE或*（表示所有方法）
     */
    @TableField("method")
    private String method;

    /**
     * 每秒请求上限（QPS）
     * 资源级限流配置，null表示不限制
     */
    @TableField("qps_limit")
    private Integer qpsLimit;

    /**
     * 令牌桶突发容量
     * 配合QPS限流使用，允许短时间内的流量突发
     */
    @TableField("burst_capacity")
    private Integer burstCapacity;

    /**
     * 每日请求配额
     * 单个凭证每日可调用此资源的最大次数，null表示不限制
     */
    @TableField("daily_quota")
    private Integer dailyQuota;

    /**
     * 并发请求限制
     * 单个凭证同时并发请求的最大数量，null表示不限制
     */
    @TableField("concurrency_limit")
    private Integer concurrencyLimit;

    /**
     * 资源状态
     * 1-启用，0-禁用
     */
    @TableField("status")
    private Integer status;

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