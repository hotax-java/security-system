package com.webapp.security.core.model;

/**
 * 基础错误码枚举
 * 
 * 错误码区间分配：
 * - 10000-19999: 系统通用错误
 * - 20000-29999: 认证/授权相关错误 (SSO模块)
 * - 30000-39999: 用户管理相关错误 (Admin模块)
 * - 40000-49999: 其他业务流程错误
 * - 50000-59999: 数据处理相关错误
 */
public interface ErrorCode {

    String INTERNAL = "INTERNAL_ERROR ";
    String UNKNOWN = "UNKNOWN_ERROR";

    String BIZ = "BIZ_ERROR_%s";
}