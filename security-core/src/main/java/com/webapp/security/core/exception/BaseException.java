package com.webapp.security.core.exception;

import com.webapp.security.core.model.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    protected final String code;
    public BaseException(String code, String message) {
        super(message);
        this.code = code;
    }
}