package com.webapp.security.core.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class IllegalArgumentExtendedException extends BaseException  {

    private static final long serialVersionUID = 1L;

    private final String fieldName;

    public IllegalArgumentExtendedException(String code, String fieldName, String errorMessage) {
        super(code,  String.format("字段【%s】错误：%s", fieldName, errorMessage));
        this.fieldName = fieldName;
    }
}